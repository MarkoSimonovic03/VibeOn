package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.ModerationRemovalRequest;
import com.metropolitan.VibeOn.dto.ModerationRemovalResponseDto;
import com.metropolitan.VibeOn.dto.FollowModerationRequest;
import com.metropolitan.VibeOn.dto.FollowModerationResponseDto;
import com.metropolitan.VibeOn.dto.UserSuspensionRequest;
import com.metropolitan.VibeOn.dto.UserSuspensionResponseDto;
import com.metropolitan.VibeOn.dto.ModerationHistoryDto;
import com.metropolitan.VibeOn.dto.ReportDismissalRequest;
import com.metropolitan.VibeOn.dto.ReportDismissalResponseDto;
import com.metropolitan.VibeOn.dto.ContentRestorationRequest;
import com.metropolitan.VibeOn.dto.ContentRestorationResponseDto;
import com.metropolitan.VibeOn.dto.MentionModerationResponseDto;
import com.metropolitan.VibeOn.entity.*;
import com.metropolitan.VibeOn.exception.ModerationTargetNotFoundException;
import com.metropolitan.VibeOn.exception.ReportTargetNotFoundException;
import com.metropolitan.VibeOn.repository.*;
import com.metropolitan.VibeOn.service.ModerationService;
import com.metropolitan.VibeOn.service.MentionService;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModerationServiceImpl implements ModerationService {
    private final ModerationActionRepository moderationActionRepository;
    private final PostRepository postRepository;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final PostCommentRepository postCommentRepository;
    private final StoryRepository storyRepository;
    private final ChatRepository chatRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final AiModerationFlagRepository aiModerationFlagRepository;
    private final UserLookupService userLookupService;
    private final MentionService mentionService;

    @Override
    @Transactional
    @PreAuthorize("hasRole('MODERATOR')")
    public MentionModerationResponseDto removeMentions(
            ModerationTargetType targetType,
            Long targetId,
            ModerationRemovalRequest request
    ) {
        if (targetType != ModerationTargetType.POST && targetType != ModerationTargetType.COMMENT) {
            throw new IllegalArgumentException("Mentions can only be moderated in posts or comments.");
        }

        User moderator = userLookupService.getCurrentUser();
        Report linkedReport = resolveLinkedReport(targetType, targetId, request.reportId());
        AiModerationFlag linkedAiFlag = resolveAiFlagForContent(targetType, targetId, request.aiFlagId());
        if (linkedReport == null && linkedAiFlag == null) {
            throw new IllegalArgumentException("Mention removal requires a mention-abuse report or AI flag.");
        }
        String affectedUsername = null;
        if (linkedReport != null) {
            if (linkedReport.getReason() != ReportReason.MENTION_ABUSE
                    || linkedReport.getAffectedUsername() == null) {
                throw new IllegalArgumentException("The linked report is not a mention-abuse report.");
            }
            affectedUsername = linkedReport.getAffectedUsername();
        }
        if (linkedAiFlag != null && linkedAiFlag.getMentionAbuseConfidence() <= 0) {
            throw new IllegalArgumentException("The linked AI flag does not identify mention abuse.");
        }

        User owner;
        String originalContent;
        String imageUrl = null;
        Long contextId = null;
        LocalDateTime targetCreatedAt;
        if (targetType == ModerationTargetType.POST) {
            Post post = postRepository.findById(targetId)
                    .orElseThrow(ModerationTargetNotFoundException::new);
            owner = post.getUser();
            originalContent = post.getDescription();
            imageUrl = firstImage(post);
            targetCreatedAt = post.getCreatedAt();
        } else {
            PostComment comment = postCommentRepository.findById(targetId)
                    .orElseThrow(ModerationTargetNotFoundException::new);
            owner = comment.getUser();
            originalContent = comment.getContent();
            contextId = comment.getPost().getId();
            targetCreatedAt = comment.getCreatedAt();
        }

        if (owner.getId().equals(moderator.getId())) {
            throw new AccessDeniedException("Use the normal content controls for your own content.");
        }

        String moderatedContent = affectedUsername == null
                ? mentionService.removeMentionLinks(originalContent)
                : mentionService.removeMentionLinks(originalContent, affectedUsername);
        if (Objects.equals(moderatedContent, originalContent)) {
            throw new IllegalArgumentException("The selected content has no mentions to remove.");
        }

        if (targetType == ModerationTargetType.POST) {
            Post post = postRepository.findById(targetId).orElseThrow(ModerationTargetNotFoundException::new);
            post.setDescription(moderatedContent);
            postRepository.save(post);
        } else {
            PostComment comment = postCommentRepository.findById(targetId)
                    .orElseThrow(ModerationTargetNotFoundException::new);
            comment.setContent(moderatedContent);
            postCommentRepository.save(comment);
        }

        LocalDateTime moderatedAt = LocalDateTime.now();
        markAiFlagActioned(linkedAiFlag, moderatedAt);

        ModerationAction action = new ModerationAction();
        action.setModerator(moderator);
        action.setTargetType(targetType == ModerationTargetType.POST
                ? ModerationTargetType.POST_MENTION_REMOVAL
                : ModerationTargetType.COMMENT_MENTION_REMOVAL);
        action.setTargetId(targetId);
        action.setTargetUsername(owner.getUsername());
        action.setTargetContent(originalContent);
        action.setResultContent(moderatedContent);
        action.setTargetImageUrl(imageUrl);
        action.setTargetContextId(contextId);
        action.setTargetCreatedAt(targetCreatedAt);
        action.setReason(request.reason().trim());
        action.setReport(linkedReport);
        action.setCreatedAt(moderatedAt);
        moderationActionRepository.save(action);

        closeMatchingReports(targetType == ModerationTargetType.POST
                ? ReportTargetType.POST
                : ReportTargetType.COMMENT, targetId);

        return new MentionModerationResponseDto(
                targetType, targetId, originalContent, moderatedContent, action.getReason(), moderatedAt
        );
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('MODERATOR')")
    public ModerationRemovalResponseDto removeContent(
            ModerationTargetType targetType,
            Long targetId,
            ModerationRemovalRequest request
    ) {
        User moderator = userLookupService.getCurrentUser();
        Report linkedReport = resolveLinkedReport(targetType, targetId, request.reportId());
        AiModerationFlag linkedAiFlag = resolveAiFlagForContent(targetType, targetId, request.aiFlagId());
        markAiFlagActioned(linkedAiFlag, LocalDateTime.now());
        RemovalTarget target = switch (targetType) {
            case POST -> removePost(targetId);
            case MESSAGE -> removeMessage(targetId);
            case COMMENT -> removeComment(targetId);
            case STORY -> removeStory(targetId);
            case USER_SUSPENSION, FOLLOW_RELATIONSHIP, AI_FLAG_REVIEW, AI_MESSAGE_FLAG_REVIEW,
                    AI_COMMENT_FLAG_REVIEW, AI_STORY_FLAG_REVIEW, REPORT_DISMISSAL, POST_RESTORATION,
                    POST_MENTION_REMOVAL, COMMENT_MENTION_REMOVAL ->
                    throw new IllegalArgumentException("Unsupported content type.");
        };

        if (target.owner().getId().equals(moderator.getId())) {
            throw new AccessDeniedException("Use the normal content controls for your own content.");
        }

        LocalDateTime removedAt = LocalDateTime.now();
        ModerationAction action = new ModerationAction();
        action.setModerator(moderator);
        action.setTargetType(targetType);
        action.setTargetId(targetId);
        action.setTargetUsername(target.owner().getUsername());
        applySnapshot(action, target);
        action.setReason(request.reason().trim());
        action.setReport(linkedReport);
        action.setCreatedAt(removedAt);
        moderationActionRepository.save(action);

        closeMatchingReports(targetType, targetId);
        return new ModerationRemovalResponseDto(
                targetType,
                targetId,
                action.getReason(),
                removedAt,
                target.chatId()
        );
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('MODERATOR')")
    public UserSuspensionResponseDto suspendUser(Long userId, UserSuspensionRequest request) {
        User moderator = userLookupService.getCurrentUser();
        User target = userRepository.findById(userId)
                .orElseThrow(ModerationTargetNotFoundException::new);
        requireOrdinaryOtherUser(moderator, target);
        Report linkedReport = resolveUserReport(userId, request.reportId());
        AiModerationFlag linkedAiFlag = resolveAiFlagForUser(userId, request.aiFlagId());

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime suspendedUntil = now.plusHours(request.durationHours());
        if (target.getSuspendedUntil() != null && target.getSuspendedUntil().isAfter(suspendedUntil)) {
            suspendedUntil = target.getSuspendedUntil();
        }
        target.setSuspendedUntil(suspendedUntil);
        target.setSuspensionReason(request.reason().trim());

        ModerationAction action = new ModerationAction();
        action.setModerator(moderator);
        action.setTargetType(ModerationTargetType.USER_SUSPENSION);
        action.setTargetId(target.getId());
        action.setTargetUsername(target.getUsername());
        action.setTargetContent((target.getName() + " " + target.getLastName()).trim());
        action.setTargetImageUrl(target.getProfileImageUrl());
        action.setTargetCreatedAt(target.getCreatedAt() == null ? null : target.getCreatedAt().atStartOfDay());
        action.setReason(request.reason().trim());
        action.setReport(linkedReport);
        action.setCreatedAt(now);
        action.setExpiresAt(suspendedUntil);
        moderationActionRepository.save(action);
        markAiFlagActioned(linkedAiFlag, now);
        closeMatchingReports(ReportTargetType.USER, target.getId());

        return new UserSuspensionResponseDto(
                target.getId(), target.getUsername(), action.getReason(), suspendedUntil
        );
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('MODERATOR')")
    public FollowModerationResponseDto removeFollow(
            Long followerId, Long followeeId, FollowModerationRequest request
    ) {
        User moderator = userLookupService.getCurrentUser();
        Follow follow = followRepository.findByFollowerIdAndFolloweeId(followerId, followeeId)
                .orElseThrow(ModerationTargetNotFoundException::new);
        requireOrdinaryOtherUser(moderator, follow.getFollower());
        requireOrdinaryOtherUser(moderator, follow.getFollowee());

        User follower = follow.getFollower();
        User followee = follow.getFollowee();
        followRepository.delete(follow);

        LocalDateTime removedAt = LocalDateTime.now();
        ModerationAction action = new ModerationAction();
        action.setModerator(moderator);
        action.setTargetType(ModerationTargetType.FOLLOW_RELATIONSHIP);
        action.setTargetId(follower.getId());
        action.setTargetUsername(follower.getUsername());
        action.setSecondaryTargetId(followee.getId());
        action.setSecondaryTargetUsername(followee.getUsername());
        action.setTargetContent((follower.getName() + " " + follower.getLastName()).trim());
        action.setTargetImageUrl(follower.getProfileImageUrl());
        action.setReason(request.reason().trim());
        action.setCreatedAt(removedAt);
        moderationActionRepository.save(action);

        return new FollowModerationResponseDto(
                follower.getId(), follower.getUsername(), followee.getId(), followee.getUsername(),
                action.getReason(), removedAt
        );
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('MODERATOR')")
    public ReportDismissalResponseDto dismissReport(Long reportId, ReportDismissalRequest request) {
        User moderator = userLookupService.getCurrentUser();
        Report report = reportRepository.findByIdAndActiveTrue(reportId)
                .orElseThrow(ReportTargetNotFoundException::new);
        LocalDateTime dismissedAt = LocalDateTime.now();

        ModerationAction action = new ModerationAction();
        action.setModerator(moderator);
        action.setTargetType(ModerationTargetType.REPORT_DISMISSAL);
        action.setTargetId(report.getTargetId());
        TargetSnapshot target = resolveReportedTarget(report);
        action.setTargetUsername(target.username());
        applySnapshot(action, target);
        action.setReason(request.reason().trim());
        action.setReport(report);
        action.setCreatedAt(dismissedAt);
        moderationActionRepository.save(action);

        report.setActive(false);
        reportRepository.save(report);

        return new ReportDismissalResponseDto(
                report.getId(), report.getTargetType(), report.getTargetId(), action.getReason(), dismissedAt
        );
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MODERATOR')")
    public List<ModerationHistoryDto> getHistory() {
        List<ModerationAction> actions = moderationActionRepository.findAllByOrderByCreatedAtDesc();
        Map<Long, Long> restoredByAction = actions.stream()
                .filter(action -> action.getOriginalAction() != null)
                .collect(Collectors.toMap(
                        action -> action.getOriginalAction().getId(),
                        ModerationAction::getId,
                        (first, ignored) -> first
                ));
        return actions.stream()
                .map(action -> toHistoryDto(action, restoredByAction.get(action.getId())))
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('MODERATOR')")
    public ContentRestorationResponseDto restoreContent(Long actionId, ContentRestorationRequest request) {
        ModerationAction original = moderationActionRepository.findById(actionId)
                .orElseThrow(ModerationTargetNotFoundException::new);
        if (original.getTargetType() != ModerationTargetType.POST
                || moderationActionRepository.existsByOriginalActionId(actionId)) {
            throw new IllegalArgumentException("This moderation action cannot be restored.");
        }
        if (postRepository.restoreDeletedById(original.getTargetId()) != 1) {
            throw new IllegalArgumentException("The removed post is no longer restorable.");
        }

        LocalDateTime restoredAt = LocalDateTime.now();
        ModerationAction restoration = new ModerationAction();
        restoration.setModerator(userLookupService.getCurrentUser());
        restoration.setTargetType(ModerationTargetType.POST_RESTORATION);
        restoration.setTargetId(original.getTargetId());
        restoration.setTargetUsername(original.getTargetUsername());
        restoration.setTargetContent(original.getTargetContent());
        restoration.setTargetImageUrl(original.getTargetImageUrl());
        restoration.setTargetContextId(original.getTargetContextId());
        restoration.setTargetCreatedAt(original.getTargetCreatedAt());
        restoration.setReason(request.reason().trim());
        restoration.setReport(original.getReport());
        restoration.setOriginalAction(original);
        restoration.setCreatedAt(restoredAt);
        moderationActionRepository.save(restoration);

        return new ContentRestorationResponseDto(
                restoration.getId(), restoration.getTargetId(), restoration.getReason(), restoredAt, original.getId()
        );
    }

    private ModerationHistoryDto toHistoryDto(ModerationAction action, Long restoredByActionId) {
        String actionType = switch (action.getTargetType()) {
            case USER_SUSPENSION -> "USER_SUSPENSION";
            case FOLLOW_RELATIONSHIP -> "FOLLOW_REMOVAL";
            case POST, MESSAGE, COMMENT, STORY -> "CONTENT_REMOVAL";
            case AI_FLAG_REVIEW, AI_MESSAGE_FLAG_REVIEW, AI_COMMENT_FLAG_REVIEW,
                    AI_STORY_FLAG_REVIEW -> "AI_REVIEW";
            case REPORT_DISMISSAL -> "REPORT_DISMISSAL";
            case POST_RESTORATION -> "CONTENT_RESTORATION";
            case POST_MENTION_REMOVAL, COMMENT_MENTION_REMOVAL -> "MENTION_MODERATION";
        };
        return new ModerationHistoryDto(
                action.getId(),
                actionType,
                action.getTargetType(),
                action.getTargetId(),
                action.getTargetUsername(),
                action.getSecondaryTargetId(),
                action.getSecondaryTargetUsername(),
                action.getModerator().getUsername(),
                action.getModerator().getProfileImageUrl(),
                userRepository.findByUsername(action.getTargetUsername())
                        .map(User::getProfileImageUrl)
                        .orElse(null),
                action.getReason(),
                action.getCreatedAt(),
                action.getExpiresAt(),
                action.getReport() == null ? null : action.getReport().getId(),
                action.getReport() == null ? null : action.getReport().getTargetType(),
                action.getReport() == null ? null : action.getReport().getAffectedUserId(),
                action.getReport() == null ? null : action.getReport().getAffectedUsername(),
                action.getTargetContent(),
                action.getResultContent(),
                action.getTargetImageUrl(),
                action.getTargetContextId(),
                action.getTargetCreatedAt(),
                action.getOriginalAction() == null ? null : action.getOriginalAction().getId(),
                restoredByActionId,
                action.getTargetType() == ModerationTargetType.POST
                        && restoredByActionId == null
                        && postRepository.countDeletedById(action.getTargetId()) > 0
        );
    }

    private TargetSnapshot resolveReportedTarget(Report report) {
        return switch (report.getTargetType()) {
            case POST -> postRepository.findById(report.getTargetId())
                    .map(post -> new TargetSnapshot(
                            post.getUser(), post.getUser().getUsername(), post.getDescription(),
                            firstImage(post), null, post.getCreatedAt()
                    ))
                    .orElseGet(TargetSnapshot::unavailable);
            case MESSAGE -> messageRepository.findById(report.getTargetId())
                    .map(message -> new TargetSnapshot(
                            message.getUser(), message.getUser().getUsername(), message.getContent(),
                            null, message.getChat().getId(), message.getCreatedAt()
                    ))
                    .orElseGet(TargetSnapshot::unavailable);
            case USER -> userRepository.findById(report.getTargetId())
                    .map(user -> new TargetSnapshot(
                            user, user.getUsername(), (user.getName() + " " + user.getLastName()).trim(),
                            user.getProfileImageUrl(), null,
                            user.getCreatedAt() == null ? null : user.getCreatedAt().atStartOfDay()
                    ))
                    .orElseGet(TargetSnapshot::unavailable);
            case COMMENT -> postCommentRepository.findById(report.getTargetId())
                    .map(comment -> new TargetSnapshot(
                            comment.getUser(), comment.getUser().getUsername(), comment.getContent(),
                            null, comment.getPost().getId(), comment.getCreatedAt()
                    ))
                    .orElseGet(TargetSnapshot::unavailable);
            case STORY -> storyRepository.findById(report.getTargetId())
                    .map(story -> new TargetSnapshot(
                            story.getUser(), story.getUser().getUsername(), null,
                            story.getImageUrl(), null, story.getCreatedAt()
                    ))
                    .orElseGet(TargetSnapshot::unavailable);
        };
    }

    private RemovalTarget removePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(ModerationTargetNotFoundException::new);
        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
        aiModerationFlagRepository.findByPostId(postId).ifPresent(flag -> {
            flag.setStatus(AiModerationFlagStatus.ACTIONED);
            flag.setReviewedAt(LocalDateTime.now());
        });
        return new RemovalTarget(
                post.getUser(), post.getUser().getUsername(), post.getDescription(), firstImage(post),
                null, post.getCreatedAt(), null
        );
    }

    private RemovalTarget removeMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(ModerationTargetNotFoundException::new);
        aiModerationFlagRepository.findByMessageId(messageId).ifPresent(aiModerationFlagRepository::delete);
        Chat chat = message.getChat();
        if (chat.getLastMessage() != null && chat.getLastMessage().getId().equals(messageId)) {
            chat.setLastMessage(messageRepository
                    .findFirstByChatIdAndIdNotOrderByCreatedAtDesc(chat.getId(), messageId)
                    .orElse(null));
            chatRepository.saveAndFlush(chat);
        }
        messageReactionRepository.deleteAllByMessageId(messageId);
        messageReactionRepository.flush();
        messageRepository.delete(message);
        return new RemovalTarget(
                message.getUser(), message.getUser().getUsername(), message.getContent(), null,
                chat.getId(), message.getCreatedAt(), chat.getId()
        );
    }

    private RemovalTarget removeComment(Long commentId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(ModerationTargetNotFoundException::new);
        aiModerationFlagRepository.findByCommentId(commentId).ifPresent(aiModerationFlagRepository::delete);
        postCommentRepository.delete(comment);
        return new RemovalTarget(
                comment.getUser(), comment.getUser().getUsername(), comment.getContent(), null,
                comment.getPost().getId(), comment.getCreatedAt(), null
        );
    }

    private RemovalTarget removeStory(Long storyId) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(ModerationTargetNotFoundException::new);
        aiModerationFlagRepository.findByStoryId(storyId).ifPresent(aiModerationFlagRepository::delete);
        storyRepository.delete(story);
        return new RemovalTarget(
                story.getUser(), story.getUser().getUsername(), null, story.getImageUrl(),
                null, story.getCreatedAt(), null
        );
    }

    private Report resolveLinkedReport(
            ModerationTargetType targetType,
            Long targetId,
            Long reportId
    ) {
        if (reportId == null) {
            return null;
        }
        Report report = reportRepository.findByIdAndActiveTrue(reportId)
                .orElseThrow(() -> new IllegalArgumentException("The report is not active."));
        ReportTargetType expectedType = switch (targetType) {
            case POST -> ReportTargetType.POST;
            case MESSAGE -> ReportTargetType.MESSAGE;
            case COMMENT -> ReportTargetType.COMMENT;
            case STORY -> ReportTargetType.STORY;
            case USER_SUSPENSION, FOLLOW_RELATIONSHIP, AI_FLAG_REVIEW, AI_MESSAGE_FLAG_REVIEW,
                    AI_COMMENT_FLAG_REVIEW, AI_STORY_FLAG_REVIEW, REPORT_DISMISSAL, POST_RESTORATION,
                    POST_MENTION_REMOVAL, COMMENT_MENTION_REMOVAL -> null;
        };
        if (expectedType == null
                || report.getTargetType() != expectedType
                || !report.getTargetId().equals(targetId)) {
            throw new IllegalArgumentException("The report does not match the selected content.");
        }
        return report;
    }

    private AiModerationFlag resolveAiFlagForContent(
            ModerationTargetType targetType,
            Long targetId,
            Long aiFlagId
    ) {
        if (aiFlagId == null) {
            return null;
        }
        AiModerationFlag flag = findPendingAiFlag(aiFlagId);
        boolean matches = switch (targetType) {
            case POST -> flag.getPost() != null && flag.getPost().getId().equals(targetId);
            case MESSAGE -> flag.getMessage() != null && flag.getMessage().getId().equals(targetId);
            case COMMENT -> flag.getComment() != null && flag.getComment().getId().equals(targetId);
            case STORY -> flag.getStory() != null && flag.getStory().getId().equals(targetId);
            case USER_SUSPENSION, FOLLOW_RELATIONSHIP, AI_FLAG_REVIEW, AI_MESSAGE_FLAG_REVIEW,
                    AI_COMMENT_FLAG_REVIEW, AI_STORY_FLAG_REVIEW, REPORT_DISMISSAL, POST_RESTORATION,
                    POST_MENTION_REMOVAL, COMMENT_MENTION_REMOVAL -> false;
        };
        if (!matches) {
            throw new IllegalArgumentException("The AI flag does not match the selected content.");
        }
        return flag;
    }

    private AiModerationFlag resolveAiFlagForUser(Long userId, Long aiFlagId) {
        if (aiFlagId == null) {
            return null;
        }
        AiModerationFlag flag = findPendingAiFlag(aiFlagId);
        User owner = ownerOfAiFlag(flag);
        if (!owner.getId().equals(userId)) {
            throw new IllegalArgumentException("The AI flag does not belong to the selected user.");
        }
        return flag;
    }

    private AiModerationFlag findPendingAiFlag(Long aiFlagId) {
        return aiModerationFlagRepository.findByIdAndStatus(aiFlagId, AiModerationFlagStatus.PENDING)
                .orElseThrow(() -> new IllegalArgumentException("The AI flag is no longer pending review."));
    }

    private void markAiFlagActioned(AiModerationFlag flag, LocalDateTime reviewedAt) {
        if (flag != null) {
            flag.setStatus(AiModerationFlagStatus.ACTIONED);
            flag.setReviewedAt(reviewedAt);
        }
    }

    private User ownerOfAiFlag(AiModerationFlag flag) {
        if (flag.getPost() != null) return flag.getPost().getUser();
        if (flag.getMessage() != null) return flag.getMessage().getUser();
        if (flag.getComment() != null) return flag.getComment().getUser();
        if (flag.getStory() != null) return flag.getStory().getUser();
        throw new IllegalArgumentException("The AI flag has no moderation target.");
    }

    private void closeMatchingReports(ModerationTargetType targetType, Long targetId) {
        ReportTargetType reportTargetType = switch (targetType) {
            case POST -> ReportTargetType.POST;
            case MESSAGE -> ReportTargetType.MESSAGE;
            case COMMENT -> ReportTargetType.COMMENT;
            case STORY -> ReportTargetType.STORY;
            case USER_SUSPENSION, FOLLOW_RELATIONSHIP, AI_FLAG_REVIEW, AI_MESSAGE_FLAG_REVIEW,
                    AI_COMMENT_FLAG_REVIEW, AI_STORY_FLAG_REVIEW, REPORT_DISMISSAL, POST_RESTORATION,
                    POST_MENTION_REMOVAL, COMMENT_MENTION_REMOVAL -> null;
        };
        if (reportTargetType == null) {
            return;
        }
        closeMatchingReports(reportTargetType, targetId);
    }

    private Report resolveUserReport(Long userId, Long reportId) {
        if (reportId == null) {
            return null;
        }
        Report report = reportRepository.findByIdAndActiveTrue(reportId)
                .orElseThrow(() -> new IllegalArgumentException("The report is not active."));
        if (report.getTargetType() != ReportTargetType.USER || !report.getTargetId().equals(userId)) {
            throw new IllegalArgumentException("The report does not match the selected user.");
        }
        return report;
    }

    private void closeMatchingReports(ReportTargetType reportTargetType, Long targetId) {
        var reports = reportRepository.findAllByTargetTypeAndTargetIdAndActiveTrue(reportTargetType, targetId);
        reports.forEach(report -> report.setActive(false));
        reportRepository.saveAll(reports);
    }

    private void requireOrdinaryOtherUser(User moderator, User target) {
        if (target.getId().equals(moderator.getId())) {
            throw new AccessDeniedException("Moderators cannot sanction themselves.");
        }
        boolean elevated = target.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_MODERATOR") || role.getName().equals("ROLE_ADMIN"));
        if (elevated) {
            throw new AccessDeniedException("Moderators cannot sanction moderator or administrator accounts.");
        }
    }

    private void applySnapshot(ModerationAction action, TargetSnapshotView target) {
        action.setTargetContent(target.content());
        action.setTargetImageUrl(target.imageUrl());
        action.setTargetContextId(target.contextId());
        action.setTargetCreatedAt(target.createdAt());
    }

    private String firstImage(Post post) {
        return post.getEffectiveImageUrls().stream().findFirst().orElse(null);
    }

    private record TargetSnapshot(
            User owner,
            String username,
            String content,
            String imageUrl,
            Long contextId,
            LocalDateTime createdAt
    ) implements TargetSnapshotView {
        private static TargetSnapshot unavailable() {
            return new TargetSnapshot(null, "unavailable", null, null, null, null);
        }
    }

    private record RemovalTarget(
            User owner,
            String username,
            String content,
            String imageUrl,
            Long contextId,
            LocalDateTime createdAt,
            Long chatId
    ) implements TargetSnapshotView {
    }

    private interface TargetSnapshotView {
        String content();
        String imageUrl();
        Long contextId();
        LocalDateTime createdAt();
    }
}
