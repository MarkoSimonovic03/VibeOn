package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.FollowActionDto;
import com.metropolitan.VibeOn.dto.FollowRequestDto;
import com.metropolitan.VibeOn.dto.UpdatePrivacyDto;
import com.metropolitan.VibeOn.dto.RelationshipUserDto;
import com.metropolitan.VibeOn.service.FollowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/follows")
public class FollowController {
    private final FollowService followService;

    @PostMapping("/{userId}")
    public ResponseEntity<?> toggleFollow(@PathVariable @Positive Long userId) {
        try {
            FollowActionDto result = followService.toggleFollow(userId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred.");
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<java.util.List<FollowRequestDto>> getPendingRequests() {
        return ResponseEntity.ok(followService.getPendingRequests());
    }

    @PostMapping("/requests/{requestId}/approve")
    public ResponseEntity<Void> approveRequest(@PathVariable @Positive Long requestId) {
        followService.approveRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/requests/{requestId}")
    public ResponseEntity<Void> rejectRequest(@PathVariable @Positive Long requestId) {
        followService.rejectRequest(requestId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{targetUserId}/request")
    public ResponseEntity<Void> cancelRequest(@PathVariable @Positive Long targetUserId) {
        followService.cancelRequest(targetUserId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/privacy")
    public ResponseEntity<java.util.Map<String, Boolean>> updatePrivacy(
            @Valid @RequestBody UpdatePrivacyDto request
    ) {
        return ResponseEntity.ok(java.util.Map.of(
                "isPrivate", followService.updatePrivacy(request.isPrivate())
        ));
    }

    @GetMapping("/{username}/followers")
    public ResponseEntity<java.util.List<RelationshipUserDto>> getFollowers(@PathVariable String username) {
        return ResponseEntity.ok(followService.getFollowers(username));
    }

    @GetMapping("/{username}/following")
    public ResponseEntity<java.util.List<RelationshipUserDto>> getFollowing(@PathVariable String username) {
        return ResponseEntity.ok(followService.getFollowing(username));
    }

    @ExceptionHandler(java.util.NoSuchElementException.class)
    public ResponseEntity<String> handleMissingRequest(java.util.NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }
}
