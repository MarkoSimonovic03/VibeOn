package com.metropolitan.VibeOn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostBookmarkResponseDto {
    private Long postId;
    private boolean bookmarkedByCurrentUser;
}
