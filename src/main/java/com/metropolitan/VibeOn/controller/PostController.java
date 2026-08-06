package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.CreatePostDto;
import com.metropolitan.VibeOn.dto.PostLikeResponseDto;
import com.metropolitan.VibeOn.dto.SinglePostDto;
import com.metropolitan.VibeOn.dto.UpdatePostDto;
import com.metropolitan.VibeOn.service.PostService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    @PostMapping()
    public ResponseEntity<?> createPost(@RequestPart("image") MultipartFile image,
                                        @Valid @RequestPart("post") CreatePostDto createPostDto) {
        try {
            SinglePostDto createdPost = postService.createPost(image, createPostDto.getDescription());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid post data.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable @Positive Long id) {
        try {
            return ResponseEntity.ok(postService.getPostById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @GetMapping("/user/{username}")
    public ResponseEntity<?> getPostsByUsername(@PathVariable String username) {
        try {
            return ResponseEntity.ok(postService.getPostsByUsername(username));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @GetMapping()
    public ResponseEntity<?> getAllPosts() {
        try {
            return ResponseEntity.ok(postService.getAllPosts());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @GetMapping("/followeePosts")
    public ResponseEntity<?> getAllPostsByFollowedUser() {
        try {
            return ResponseEntity.ok(postService.getAllPostsByFolloweeUser());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @PatchMapping("/{id}")
    public ResponseEntity<?> updatePost(
            @PathVariable @Positive Long id,
            @Valid @RequestBody UpdatePostDto request
    ) {
        try {
            SinglePostDto updatedPost = postService.updatePost(id, request.getNewDescription());
            return ResponseEntity.ok(updatedPost);
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePost(@PathVariable @Positive Long id) {
        try {
            postService.softDeletePost(id);
            return ResponseEntity.noContent().build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @PostMapping("/{id}/likes")
    public ResponseEntity<PostLikeResponseDto> likePost(@PathVariable @Positive Long id) {
        try {
            return ResponseEntity.ok(postService.likePost(id));
        } catch (org.springframework.security.access.AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException exception) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}/likes")
    public ResponseEntity<PostLikeResponseDto> unlikePost(@PathVariable @Positive Long id) {
        try {
            return ResponseEntity.ok(postService.unlikePost(id));
        } catch (org.springframework.security.access.AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        } catch (RuntimeException exception) {
            return ResponseEntity.notFound().build();
        }
    }
}
