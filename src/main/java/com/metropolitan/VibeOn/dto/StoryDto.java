package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.Story;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StoryDto {
    private Long id;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String username;
    private String profileImageUrl;
    private boolean ownStory;

    public static StoryDto fromStory(Story story, Long currentUserId) {
        return new StoryDto(
                story.getId(),
                story.getImageUrl(),
                story.getCreatedAt(),
                story.getExpiresAt(),
                story.getUser().getUsername(),
                story.getUser().getProfileImageUrl(),
                story.getUser().getId().equals(currentUserId)
        );
    }
}
