package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.PostCommentDto;

import java.util.List;

public interface PostCommentService {
    List<PostCommentDto> getComments(Long postId);

    PostCommentDto addComment(Long postId, String content);

    void deleteComment(Long postId, Long commentId);
}
