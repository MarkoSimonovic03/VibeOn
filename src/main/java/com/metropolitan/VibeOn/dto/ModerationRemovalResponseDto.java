package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.ModerationTargetType;

import java.time.LocalDateTime;

public record ModerationRemovalResponseDto(
        ModerationTargetType targetType,
        Long targetId,
        String reason,
        LocalDateTime removedAt,
        Long targetChatId
) {
}
