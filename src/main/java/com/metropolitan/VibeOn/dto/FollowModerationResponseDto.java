package com.metropolitan.VibeOn.dto;

import java.time.LocalDateTime;

public record FollowModerationResponseDto(
        Long followerId,
        String followerUsername,
        Long followeeId,
        String followeeUsername,
        String reason,
        LocalDateTime removedAt
) {
}
