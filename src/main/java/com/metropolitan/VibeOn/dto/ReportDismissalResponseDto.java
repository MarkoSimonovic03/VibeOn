package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.ReportTargetType;

import java.time.LocalDateTime;

public record ReportDismissalResponseDto(
        Long reportId,
        ReportTargetType targetType,
        Long targetId,
        String reason,
        LocalDateTime dismissedAt
) {
}
