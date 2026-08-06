package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.CreatePostCommentDto;
import com.metropolitan.VibeOn.dto.PostCommentDto;
import com.metropolitan.VibeOn.service.PostCommentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/posts/{postId}/comments")
public class PostCommentController {
    private final PostCommentService postCommentService;

    @GetMapping
    public ResponseEntity<List<PostCommentDto>> getComments(
            @PathVariable @Positive Long postId
    ) {
        return ResponseEntity.ok(postCommentService.getComments(postId));
    }

    @PostMapping
    public ResponseEntity<PostCommentDto> addComment(
            @PathVariable @Positive Long postId,
            @Valid @RequestBody CreatePostCommentDto request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(postCommentService.addComment(postId, request.getContent()));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable @Positive Long postId,
            @PathVariable @Positive Long commentId
    ) {
        postCommentService.deleteComment(postId, commentId);
        return ResponseEntity.noContent().build();
    }
}
