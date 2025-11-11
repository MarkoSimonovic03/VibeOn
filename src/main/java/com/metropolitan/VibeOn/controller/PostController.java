package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.AccessDeniedException;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostController {
    private final PostService postService;

    // Endpoint for creating new post
    @PostMapping()
    private ResponseEntity<?> createPost(@RequestParam("image") MultipartFile image,
                                         @RequestParam("description") String description) {
        try {
            Post createdPost = postService.createPost(image, description);
            //TODO fix return data, make DTO for it!
            return ResponseEntity.ok().body(createdPost);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("Invalid data: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePost(@PathVariable Long id) {
        try {
            postService.softDeletePost(id);
            return ResponseEntity.status(204).body("Post deleted successfully.");
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Post not found.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPostById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok().body(postService.getPostById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Post not found.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getPostsByUserId(@PathVariable Long userId) {
        try {
            return ResponseEntity.ok().body(postService.getPostsByUserId(userId));
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GetMapping()
    public ResponseEntity<?> getAllPosts() {
        try {
            return ResponseEntity.ok().body(postService.getAllPosts());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/followeePosts")
    public ResponseEntity<?> getAllPostsByFollowedUser() {
        try {
            return ResponseEntity.ok().body(postService.getAllPostsByFolloweeUser());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }


    @PatchMapping("/{id}")
    public ResponseEntity<?> updatePost(@PathVariable Long id, @RequestBody Map<String,String> newDescription) {
        try {
            postService.updatePost(id, newDescription.get("newDescription"));
            return ResponseEntity.status(200).body("Post updated successfully.");
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Post not found.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }
}
