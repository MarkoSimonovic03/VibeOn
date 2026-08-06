package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.CreateReportDto;
import com.metropolitan.VibeOn.dto.ReportResponseDto;
import com.metropolitan.VibeOn.entity.Message;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.Report;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.exception.DuplicateReportException;
import com.metropolitan.VibeOn.exception.ReportTargetNotFoundException;
import com.metropolitan.VibeOn.repository.ChatRepository;
import com.metropolitan.VibeOn.repository.MessageRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.repository.ReportRepository;
import com.metropolitan.VibeOn.service.ReportService;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {
    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserLookupService userLookupService;

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
        report.setCreatedAt(LocalDateTime.now());

        return ReportResponseDto.fromReport(reportRepository.save(report));
    }

    private void validateTarget(CreateReportDto request, User reporter) {
        switch (request.getTargetType()) {
            case POST -> validatePost(request.getTargetId(), reporter);
            case MESSAGE -> validateMessage(request.getTargetId(), reporter);
            case USER -> validateUser(request.getTargetId(), reporter);
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

    private void rejectOwnTarget(User owner, User reporter) {
        if (owner.getId().equals(reporter.getId())) {
            throw new AccessDeniedException("You cannot report your own content or account.");
        }
    }

    private String normalizeDescription(String description) {
        return StringUtils.hasText(description) ? description.trim() : null;
    }
}
