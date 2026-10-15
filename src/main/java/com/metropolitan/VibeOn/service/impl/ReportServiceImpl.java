package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.CreateReportDto;
import com.metropolitan.VibeOn.dto.ModeratorReportDto;
import com.metropolitan.VibeOn.dto.ReportResponseDto;
import com.metropolitan.VibeOn.entity.Message;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.PostComment;
import com.metropolitan.VibeOn.entity.Report;
import com.metropolitan.VibeOn.entity.ReportTargetType;
import com.metropolitan.VibeOn.entity.ReportReason;
import com.metropolitan.VibeOn.entity.Story;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.exception.DuplicateReportException;
import com.metropolitan.VibeOn.exception.ReportTargetNotFoundException;
import com.metropolitan.VibeOn.repository.ChatRepository;
import com.metropolitan.VibeOn.repository.MessageRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.repository.PostCommentRepository;
import com.metropolitan.VibeOn.repository.ReportRepository;
import com.metropolitan.VibeOn.repository.StoryRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.service.ReportService;
import com.metropolitan.VibeOn.service.MentionService;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final PostCommentRepository postCommentRepository;
    private final StoryRepository storyRepository;
    private final UserRepository userRepository;
    private final UserLookupService userLookupService;
    private final MentionService mentionService;

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public ReportResponseDto createReport(CreateReportDto request) {
        User reporter = userLookupService.getCurrentUser();
        validateTarget(request, reporter);

        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetIdAndActiveTrue(
                reporter.getId(),
                request.getTargetType(),
                request.getTargetId()
        )) {
            throw new DuplicateReportException();
        }

        Report report = new Report();
        report.setReporter(reporter);
        report.setTargetType(request.getTargetType());
        report.setTargetId(request.getTargetId());
        report.setReason(request.getReason());
        report.setDescription(normalizeDescription(request.getDescription()));
        if (request.getReason() == ReportReason.MENTION_ABUSE) {
            report.setAffectedUserId(reporter.getId());
            report.setAffectedUsername(reporter.getUsername());
        }
        report.setCreatedAt(LocalDateTime.now());

        return ReportResponseDto.fromReport(reportRepository.save(report));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MODERATOR')")
    public List<ModeratorReportDto> getActiveReports(ReportTargetType targetType) {
        List<Report> reports = targetType == null
                ? reportRepository.findAllByActiveTrueOrderByCreatedAtDesc()
                : reportRepository.findAllByActiveTrueAndTargetTypeOrderByCreatedAtDesc(targetType);
        return reports.stream().map(this::toModeratorReport).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('MODERATOR')")
    public ModeratorReportDto getActiveReport(Long reportId) {
        return reportRepository.findByIdAndActiveTrue(reportId)
                .map(this::toModeratorReport)
                .orElseThrow(ReportTargetNotFoundException::new);
    }

    private ModeratorReportDto toModeratorReport(Report report) {
        return ModeratorReportDto.fromReport(report, switch (report.getTargetType()) {
            case POST -> postRepository.findById(report.getTargetId())
                    .map(post -> new ModeratorReportDto.TargetPreview(
                            post.getUser().getUsername(),
                            post.getUser().getProfileImageUrl(),
                            post.getDescription(),
                            post.getImageUrl(),
                            post.getCreatedAt(),
                            null,
                            true
                    ))
                    .orElseGet(ModeratorReportDto.TargetPreview::unavailable);
            case MESSAGE -> messageRepository.findById(report.getTargetId())
                    .map(message -> new ModeratorReportDto.TargetPreview(
                            message.getUser().getUsername(),
                            message.getUser().getProfileImageUrl(),
                            message.getContent(),
                            null,
                            message.getCreatedAt(),
                            message.getChat().getId(),
                            true
                    ))
                    .orElseGet(ModeratorReportDto.TargetPreview::unavailable);
            case USER -> userRepository.findById(report.getTargetId())
                    .map(user -> new ModeratorReportDto.TargetPreview(
                            user.getUsername(),
                            user.getProfileImageUrl(),
                            user.getName() + " " + user.getLastName(),
                            user.getProfileImageUrl(),
                            user.getCreatedAt() == null
                                    ? null
                                    : user.getCreatedAt().atStartOfDay(),
                            null,
                            true
                    ))
                    .orElseGet(ModeratorReportDto.TargetPreview::unavailable);
            case COMMENT -> postCommentRepository.findById(report.getTargetId())
                    .map(comment -> new ModeratorReportDto.TargetPreview(
                            comment.getUser().getUsername(),
                            comment.getUser().getProfileImageUrl(),
                            comment.getContent(),
                            null,
                            comment.getCreatedAt(),
                            null,
                            true
                    ))
                    .orElseGet(ModeratorReportDto.TargetPreview::unavailable);
            case STORY -> storyRepository.findById(report.getTargetId())
                    .map(story -> new ModeratorReportDto.TargetPreview(
                            story.getUser().getUsername(),
                            story.getUser().getProfileImageUrl(),
                            null,
                            story.getImageUrl(),
                            story.getCreatedAt(),
                            null,
                            true
                    ))
                    .orElseGet(ModeratorReportDto.TargetPreview::unavailable);
        });
    }

    private void validateTarget(CreateReportDto request, User reporter) {
        if (request.getReason() == ReportReason.MENTION_ABUSE) {
            validateMentionAbuseTarget(request, reporter);
            return;
        }
        switch (request.getTargetType()) {
            case POST -> validatePost(request.getTargetId(), reporter);
            case MESSAGE -> validateMessage(request.getTargetId(), reporter);
            case USER -> validateUser(request.getTargetId(), reporter);
            case COMMENT -> validateComment(request.getTargetId(), reporter);
            case STORY -> validateStory(request.getTargetId(), reporter);
        }
    }

    private void validateMentionAbuseTarget(CreateReportDto request, User reporter) {
        String content = switch (request.getTargetType()) {
            case POST -> {
                Post post = postRepository.findById(request.getTargetId())
                        .orElseThrow(ReportTargetNotFoundException::new);
                rejectOwnTarget(post.getUser(), reporter);
                yield post.getDescription();
            }
            case COMMENT -> {
                PostComment comment = postCommentRepository.findById(request.getTargetId())
                        .orElseThrow(ReportTargetNotFoundException::new);
                rejectOwnTarget(comment.getUser(), reporter);
                yield comment.getContent();
            }
            case MESSAGE, USER, STORY -> throw new IllegalArgumentException(
                    "Mention abuse can only be reported for a post or comment."
            );
        };
        if (!mentionService.findValidUsernames(content).contains(reporter.getUsername())) {
            throw new AccessDeniedException("You can only report mention abuse when the content mentions you.");
        }
    }

    private void validatePost(Long postId, User reporter) {
        Post post = postRepository.findById(postId)
                .orElseThrow(ReportTargetNotFoundException::new);
        rejectOwnTarget(post.getUser(), reporter);
    }

    private void validateMessage(Long messageId, User reporter) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(ReportTargetNotFoundException::new);
        rejectOwnTarget(message.getUser(), reporter);

        if (!chatRepository.existsByIdAndUsersUsername(message.getChat().getId(), reporter.getUsername())) {
            throw new AccessDeniedException("The message is not in one of your chats.");
        }
    }

    private void validateUser(Long userId, User reporter) {
        User reportedUser;
        try {
            reportedUser = userLookupService.getById(userId);
        } catch (RuntimeException exception) {
            throw new ReportTargetNotFoundException();
        }
        rejectOwnTarget(reportedUser, reporter);
    }

    private void validateComment(Long commentId, User reporter) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(ReportTargetNotFoundException::new);
        rejectOwnTarget(comment.getUser(), reporter);
    }

    private void validateStory(Long storyId, User reporter) {
        Story story = storyRepository.findById(storyId)
                .orElseThrow(ReportTargetNotFoundException::new);
        rejectOwnTarget(story.getUser(), reporter);
    }

    private void rejectOwnTarget(User owner, User reporter) {
        if (owner.getId().equals(reporter.getId())) {
            throw new AccessDeniedException("You cannot report your own content or account.");
        }
    }

    private String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }
}
