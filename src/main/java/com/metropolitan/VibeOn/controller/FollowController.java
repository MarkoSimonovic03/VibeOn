package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.service.FollowService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/follows")
public class FollowController {
    private final FollowService followService;

    @PostMapping("/{userId}")
    public ResponseEntity<?> toggleFollow(@PathVariable @Positive Long userId) {
        try {
            followService.toggleFollow(userId);
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }
}
