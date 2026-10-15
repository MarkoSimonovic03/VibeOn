package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.AiModerationFlagDto;
import com.metropolitan.VibeOn.entity.*;
import com.metropolitan.VibeOn.exception.ModerationTargetNotFoundException;
import com.metropolitan.VibeOn.repository.AiModerationFlagRepository;
import com.metropolitan.VibeOn.repository.ModerationActionRepository;
import com.metropolitan.VibeOn.service.AiModerationService;
import com.metropolitan.VibeOn.service.PostModerationAnalysis;
import com.metropolitan.VibeOn.service.PostModerationAnalyzer;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiModerationServiceImpl implements AiModerationService {
    private final PostModerationAnalyzer analyzer;
    private final AiModerationFlagRepository flagRepository;
    private final ModerationActionRepository moderationActionRepository;
    private final UserLookupService userLookupService;

    @Override
    @Transactional
    public void analyzeAndFlag(Post post, List<MultipartFile> images) {
        try {
            PostModerationAnalysis analysis = analyzer.analyze(post.getDescription(), images);
            if (!analysis.suspicious()) return;

            AiModerationFlag flag = flagRepository.findByPostId(post.getId()).orElseGet(AiModerationFlag::new);
            flag.setPost(post);
            applyAnalysis(flag, analysis, true);
        } catch (Exception exception) {
            log.warn("Post {} moderation analysis failed; post creation will continue", post.getId(), exception);
        }
    }

    @Override
    @Transactional
    public void analyzeAndFlag(PostComment comment) {
        try {
            PostModerationAnalysis analysis = analyzer.analyze(comment.getContent(), List.of());
            if (!analysis.suspicious()) return;
            AiModerationFlag flag = flagRepository.findByCommentId(comment.getId()).orElseGet(AiModerationFlag::new);
            flag.setComment(comment);
            applyAnalysis(flag, analysis, false);
        } catch (Exception exception) {
            log.warn("Comment {} moderation analysis failed; comment creation will continue", comment.getId(), exception);
        }
    }

    @Override
    @Transactional
    public void analyzeAndFlag(Story story, MultipartFile image) {
        try {
            PostModerationAnalysis analysis = analyzer.analyze(null, List.of(image));
            if (!analysis.suspicious()) return;
            AiModerationFlag flag = flagRepository.findByStoryId(story.getId()).orElseGet(AiModerationFlag::new);
            flag.setStory(story);
            applyAnalysis(flag, analysis, false);
        } catch (Exception exception) {
            log.warn("Story {} moderation analysis failed; story creation will continue", story.getId(), exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MODERATOR')")
    public List<AiModerationFlagDto> getPendingFlags() {
        return flagRepository.findAllByStatusOrderByFlaggedAtDesc(AiModerationFlagStatus.PENDING)
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('MODERATOR')")
    public void approveFalsePositive(Long flagId, String reason) {
        AiModerationFlag flag = flagRepository.findByIdAndStatus(flagId, AiModerationFlagStatus.PENDING)
                .orElseThrow(ModerationTargetNotFoundException::new);
        User moderator = userLookupService.getCurrentUser();
        LocalDateTime reviewedAt = LocalDateTime.now();
        flag.setStatus(AiModerationFlagStatus.DISMISSED);
        flag.setReviewedAt(reviewedAt);

        ModerationAction action = new ModerationAction();
        action.setModerator(moderator);
        FlagTarget target = targetOf(flag);
        action.setTargetType(target.reviewType());
        action.setTargetId(target.id());
        action.setTargetUsername(target.owner().getUsername());
        action.setSecondaryTargetId(target.contextId());
        action.setTargetContextId(target.contextId());
        if (flag.getPost() != null) {
            action.setTargetContent(flag.getPost().getDescription());
            action.setTargetImageUrl(flag.getPost().getEffectiveImageUrls().stream().findFirst().orElse(null));
            action.setTargetCreatedAt(flag.getPost().getCreatedAt());
        } else if (flag.getMessage() != null) {
            action.setTargetContent(flag.getMessage().getContent());
            action.setTargetCreatedAt(flag.getMessage().getCreatedAt());
        } else if (flag.getComment() != null) {
            action.setTargetContent(flag.getComment().getContent());
            action.setTargetCreatedAt(flag.getComment().getCreatedAt());
        } else if (flag.getStory() != null) {
            action.setTargetImageUrl(flag.getStory().getImageUrl());
            action.setTargetCreatedAt(flag.getStory().getCreatedAt());
        }
        action.setReason(reason.trim());
        action.setCreatedAt(reviewedAt);
        moderationActionRepository.save(action);
    }

    private AiModerationFlagDto toDto(AiModerationFlag flag) {
        Map<AiModerationCategory, Double> confidences = new EnumMap<>(AiModerationCategory.class);
        confidences.put(AiModerationCategory.NSFW, flag.getNsfwConfidence());
        confidences.put(AiModerationCategory.VIOLENCE_BLOOD, flag.getViolenceBloodConfidence());
        confidences.put(AiModerationCategory.WEAPONS, flag.getWeaponsConfidence());
        confidences.put(AiModerationCategory.DRUGS, flag.getDrugsConfidence());
        confidences.put(AiModerationCategory.HATE_HARASSMENT, flag.getHateHarassmentConfidence());
        confidences.put(AiModerationCategory.MENTION_ABUSE, flag.getMentionAbuseConfidence());
        if (flag.getPost() != null) {
            Post post = flag.getPost();
            return new AiModerationFlagDto(
                    flag.getId(), "POST", post.getId(), null, null, null, null, null,
                    post.getUser().getId(), post.getUser().getUsername(), post.getDescription(), post.getEffectiveImageUrls(),
                    post.getCreatedAt(), flag.getFlaggedAt(), flag.getTextConfidence(),
                    Map.copyOf(confidences), flag.getSummary()
            );
        }
        if (flag.getMessage() != null) {
            Message message = flag.getMessage();
            return new AiModerationFlagDto(
                    flag.getId(), "MESSAGE", null, message.getId(), message.getChat().getId(), null, null, null,
                    message.getUser().getId(), message.getUser().getUsername(), message.getContent(), List.of(),
                    message.getCreatedAt(), flag.getFlaggedAt(), flag.getTextConfidence(),
                    Map.copyOf(confidences), flag.getSummary()
            );
        }
        if (flag.getComment() != null) {
            PostComment comment = flag.getComment();
            return new AiModerationFlagDto(
                    flag.getId(), "COMMENT", null, null, null, comment.getId(), null, comment.getPost().getId(),
                    comment.getUser().getId(), comment.getUser().getUsername(), comment.getContent(), List.of(),
                    comment.getCreatedAt(), flag.getFlaggedAt(), flag.getTextConfidence(),
                    Map.copyOf(confidences), flag.getSummary()
            );
        }
        Story story = flag.getStory();
        return new AiModerationFlagDto(
                flag.getId(), "STORY", null, null, null, null, story.getId(), null,
                story.getUser().getId(), story.getUser().getUsername(), null, List.of(story.getImageUrl()),
                story.getCreatedAt(), flag.getFlaggedAt(), flag.getTextConfidence(),
                Map.copyOf(confidences), flag.getSummary()
        );
    }

    private void applyAnalysis(AiModerationFlag flag, PostModerationAnalysis analysis, boolean merge) {
        flag.setTextConfidence(merge ? Math.max(flag.getTextConfidence(), analysis.textConfidence()) : analysis.textConfidence());
        flag.setNsfwConfidence(score(flag.getNsfwConfidence(), analysis, AiModerationCategory.NSFW, merge));
        flag.setViolenceBloodConfidence(score(flag.getViolenceBloodConfidence(), analysis, AiModerationCategory.VIOLENCE_BLOOD, merge));
        flag.setWeaponsConfidence(score(flag.getWeaponsConfidence(), analysis, AiModerationCategory.WEAPONS, merge));
        flag.setDrugsConfidence(score(flag.getDrugsConfidence(), analysis, AiModerationCategory.DRUGS, merge));
        flag.setHateHarassmentConfidence(score(flag.getHateHarassmentConfidence(), analysis, AiModerationCategory.HATE_HARASSMENT, merge));
        flag.setMentionAbuseConfidence(score(flag.getMentionAbuseConfidence(), analysis, AiModerationCategory.MENTION_ABUSE, merge));
        flag.setSummary(analysis.summary());
        flag.setStatus(AiModerationFlagStatus.PENDING);
        flag.setReviewedAt(null);
        flag.setFlaggedAt(LocalDateTime.now());
        flagRepository.save(flag);
    }

    private double score(double current, PostModerationAnalysis analysis, AiModerationCategory category, boolean merge) {
        double analyzed = analysis.confidence(category);
        return merge ? Math.max(current, analyzed) : analyzed;
    }

    private FlagTarget targetOf(AiModerationFlag flag) {
        if (flag.getPost() != null) {
            return new FlagTarget(ModerationTargetType.AI_FLAG_REVIEW, flag.getPost().getId(), flag.getPost().getUser(), null);
        }
        if (flag.getMessage() != null) {
            return new FlagTarget(ModerationTargetType.AI_MESSAGE_FLAG_REVIEW, flag.getMessage().getId(), flag.getMessage().getUser(), flag.getMessage().getChat().getId());
        }
        if (flag.getComment() != null) {
            return new FlagTarget(ModerationTargetType.AI_COMMENT_FLAG_REVIEW, flag.getComment().getId(), flag.getComment().getUser(), flag.getComment().getPost().getId());
        }
        if (flag.getStory() != null) {
            return new FlagTarget(ModerationTargetType.AI_STORY_FLAG_REVIEW, flag.getStory().getId(), flag.getStory().getUser(), null);
        }
        throw new IllegalStateException("AI moderation flag has no target.");
    }

    private record FlagTarget(ModerationTargetType reviewType, Long id, User owner, Long contextId) {
    }
}
