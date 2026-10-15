package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.Report;
import com.metropolitan.VibeOn.entity.ReportReason;
import com.metropolitan.VibeOn.entity.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ModeratorReportDto {
    private Long id;
    private ReportTargetType targetType;
    private Long targetId;
    private ReportReason reason;
    private String description;
    private Long affectedUserId;
    private String affectedUsername;
    private LocalDateTime createdAt;
    private Long reporterId;
    private String reporterUsername;
    private String reporterProfileImageUrl;
    private String targetUsername;
    private String targetProfileImageUrl;
    private String targetContent;
    private String targetImageUrl;
    private LocalDateTime targetCreatedAt;
    private Long targetChatId;
    private boolean targetAvailable;

    public static ModeratorReportDto fromReport(Report report, TargetPreview target) {
        return new ModeratorReportDto(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getDescription(),
                report.getAffectedUserId(),
                report.getAffectedUsername(),
                report.getCreatedAt(),
                report.getReporter().getId(),
                report.getReporter().getUsername(),
                report.getReporter().getProfileImageUrl(),
                target.username(),
                target.profileImageUrl(),
                target.content(),
                target.imageUrl(),
                target.createdAt(),
                target.chatId(),
                target.available()
        );
    }

    public record TargetPreview(
            String username,
            String profileImageUrl,
            String content,
            String imageUrl,
            LocalDateTime createdAt,
            Long chatId,
            boolean available
    ) {
        public static TargetPreview unavailable() {
            return new TargetPreview(null, null, null, null, null, null, false);
        }
    }
}
