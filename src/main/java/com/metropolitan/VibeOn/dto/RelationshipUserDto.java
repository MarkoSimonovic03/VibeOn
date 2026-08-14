package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.User;

public record RelationshipUserDto(
        Long id,
        String username,
        String name,
        String lastName,
        String profileImageUrl,
        boolean following,
        boolean requestPending,
        boolean currentUser
) {
    public static RelationshipUserDto fromUser(
            User user,
            boolean following,
            boolean requestPending,
            boolean currentUser
    ) {
        return new RelationshipUserDto(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getLastName(),
                user.getProfileImageUrl(),
                following,
                requestPending,
                currentUser
        );
    }
}
