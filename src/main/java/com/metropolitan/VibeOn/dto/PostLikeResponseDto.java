package com.metropolitan.VibeOn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PostLikeResponseDto {
    private Long postId;
    private long likeCount;
    private boolean likedByCurrentUser;
}
