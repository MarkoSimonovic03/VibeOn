package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.PostComment;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class PostCommentDto {
    private Long id;
    private Long postId;
    private String username;
    private String profileImageUrl;
    private String content;
    private LocalDateTime createdAt;
    @Setter
    private List<String> mentionedUsernames;

    public static PostCommentDto fromComment(PostComment comment) {
        return new PostCommentDto(
                comment.getId(),
                comment.getPost().getId(),
                comment.getUser().getUsername(),
                comment.getUser().getProfileImageUrl(),
                comment.getContent(),
                comment.getCreatedAt(),
                List.of()
        );
    }
}
