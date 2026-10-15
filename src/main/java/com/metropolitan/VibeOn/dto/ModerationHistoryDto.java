package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.ModerationTargetType;
import com.metropolitan.VibeOn.entity.ReportTargetType;

import java.time.LocalDateTime;

public record ModerationHistoryDto(
        Long id,
        String actionType,
        ModerationTargetType targetType,
        Long targetId,
        String targetUsername,
        Long secondaryTargetId,
        String secondaryTargetUsername,
        String moderatorUsername,
        String moderatorProfileImageUrl,
        String targetProfileImageUrl,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime expiresAt,
        Long reportId,
        ReportTargetType reportTargetType,
        Long affectedUserId,
        String affectedUsername,
        String targetContent,
        String resultContent,
        String targetImageUrl,
        Long targetContextId,
        LocalDateTime targetCreatedAt,
        Long originalActionId,
        Long restoredByActionId,
        boolean canRestore
) {
}
