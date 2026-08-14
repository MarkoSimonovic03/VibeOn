package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProfileDto {
    private Long id;
    private String username;
    private String name;
    private String lastName;
    private LocalDate createdAt;
    private String profileImageUrl;
    private Boolean isFollowing;
    private Boolean isPrivate;
    private boolean followRequestPending;
    private boolean canViewPosts;
    private long followerCount;
    private long followingCount;
    private boolean canViewConnections;

    public static ProfileDto fromUser(
            User user,
            boolean isFollowing,
            boolean followRequestPending,
            boolean canViewPosts,
            long followerCount,
            long followingCount
    ) {
        return new ProfileDto(
                user.getId(),
                user.getUsername(),
                user.getName(),
                user.getLastName(),
                user.getCreatedAt(),
                user.getProfileImageUrl(),
                isFollowing,
                user.isPrivate(),
                followRequestPending,
                canViewPosts,
                followerCount,
                followingCount,
                canViewPosts
        );
    }
}
