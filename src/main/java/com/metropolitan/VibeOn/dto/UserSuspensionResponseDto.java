package com.metropolitan.VibeOn.dto;

import java.time.LocalDateTime;

public record UserSuspensionResponseDto(
        Long userId,
        String username,
        String reason,
        LocalDateTime suspendedUntil
) {
}
