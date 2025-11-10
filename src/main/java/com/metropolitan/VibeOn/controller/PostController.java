package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

            return ResponseEntity.ok().body(createdPost);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(400).body("Invalid data: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }
}
