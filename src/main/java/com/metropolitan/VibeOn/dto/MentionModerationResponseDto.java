package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.ModerationTargetType;

import java.time.LocalDateTime;

public record MentionModerationResponseDto(
        ModerationTargetType targetType,
        Long targetId,
        String originalContent,
        String moderatedContent,
        String reason,
        LocalDateTime moderatedAt
) {
}
