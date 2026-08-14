package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.FollowRequest;

import java.time.LocalDateTime;

public record FollowRequestDto(
        Long id,
        Long requesterId,
        String username,
        String profileImageUrl,
        LocalDateTime createdAt
) {
    public static FollowRequestDto fromRequest(FollowRequest request) {
        return new FollowRequestDto(
                request.getId(),
                request.getRequester().getId(),
                request.getRequester().getUsername(),
                request.getRequester().getProfileImageUrl(),
                request.getCreatedAt()
        );
    }
}
