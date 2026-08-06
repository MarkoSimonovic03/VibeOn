package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.Post;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class SinglePostDto {
    private Long id;
    private String imageUrl;
    private LocalDateTime createdAt;
    private String description;
    private Long userId;
    private String username;
    private String profileImageUrl;
    private long likeCount;
    private boolean likedByCurrentUser;
    private boolean bookmarkedByCurrentUser;

    public SinglePostDto(
            Long id,
            String imageUrl,
            LocalDateTime createdAt,
            String description,
            Long userId,
            String username,
            String profileImageUrl
    ) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.createdAt = createdAt;
        this.description = description;
        this.userId = userId;
        this.username = username;
        this.profileImageUrl = profileImageUrl;
    }

    public static SinglePostDto fromPost(Post post) {
        return new SinglePostDto(
                post.getId(),
                post.getImageUrl(),
                post.getCreatedAt(),
                post.getDescription(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getProfileImageUrl()
        );
    }
}
