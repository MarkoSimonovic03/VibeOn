package com.metropolitan.VibeOn.dto;

import java.time.LocalDateTime;

public record ContentRestorationResponseDto(
        Long actionId,
        Long targetId,
        String reason,
        LocalDateTime restoredAt,
        Long originalActionId
) {
}
