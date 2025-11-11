package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follows")
public class FollowController {
    private final FollowService followService;

    @PostMapping("/{userId}")
    private ResponseEntity<?> createFollow(@PathVariable Long userId) {
        try {
            return ResponseEntity.status(200).body(followService.createFollow(userId));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Followee not found.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<?> unFollow(@PathVariable Long userId) {
        try {
            followService.unFollow(userId);
            return ResponseEntity.status(204).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Followee not found.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }
}
