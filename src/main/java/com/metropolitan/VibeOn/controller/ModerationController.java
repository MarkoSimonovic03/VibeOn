package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.ModerationRemovalRequest;
import com.metropolitan.VibeOn.dto.ModerationRemovalResponseDto;
import com.metropolitan.VibeOn.dto.FollowModerationRequest;
import com.metropolitan.VibeOn.dto.FollowModerationResponseDto;
import com.metropolitan.VibeOn.dto.UserSuspensionRequest;
import com.metropolitan.VibeOn.dto.UserSuspensionResponseDto;
import com.metropolitan.VibeOn.dto.ModerationHistoryDto;
import com.metropolitan.VibeOn.dto.ReportDismissalRequest;
import com.metropolitan.VibeOn.dto.ReportDismissalResponseDto;
import com.metropolitan.VibeOn.dto.ContentRestorationRequest;
import com.metropolitan.VibeOn.dto.ContentRestorationResponseDto;
import com.metropolitan.VibeOn.dto.MentionModerationResponseDto;
import com.metropolitan.VibeOn.entity.ModerationTargetType;
import com.metropolitan.VibeOn.service.ModerationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/moderation")
public class ModerationController {
    private final ModerationService moderationService;
    private final SimpMessagingTemplate messagingTemplate;

    @GetMapping("/history")
    public ResponseEntity<List<ModerationHistoryDto>> getHistory() {
        return ResponseEntity.ok(moderationService.getHistory());
    }

    @PostMapping("/history/{actionId}/restore")
    public ResponseEntity<ContentRestorationResponseDto> restoreContent(
            @PathVariable @Positive Long actionId,
            @Valid @RequestBody ContentRestorationRequest request
    ) {
        return ResponseEntity.ok(moderationService.restoreContent(actionId, request));
    }

    @PostMapping("/mentions/{targetType}/{targetId}/remove")
    public ResponseEntity<MentionModerationResponseDto> removeMentions(
            @PathVariable ModerationTargetType targetType,
            @PathVariable @Positive Long targetId,
            @Valid @RequestBody ModerationRemovalRequest request
    ) {
        return ResponseEntity.ok(moderationService.removeMentions(targetType, targetId, request));
    }

    @PostMapping("/reports/{reportId}/dismiss")
    public ResponseEntity<ReportDismissalResponseDto> dismissReport(
            @PathVariable @Positive Long reportId,
            @Valid @RequestBody ReportDismissalRequest request
    ) {
        return ResponseEntity.ok(moderationService.dismissReport(reportId, request));
    }

    @DeleteMapping("/content/{targetType}/{targetId}")
    public ResponseEntity<ModerationRemovalResponseDto> removeContent(
            @PathVariable ModerationTargetType targetType,
            @PathVariable @Positive Long targetId,
            @Valid @RequestBody ModerationRemovalRequest request
    ) {
        ModerationRemovalResponseDto response = moderationService.removeContent(targetType, targetId, request);
        if (response.targetChatId() != null) {
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + response.targetChatId() + "/moderation",
                    Map.of("messageId", response.targetId())
            );
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/users/{userId}/suspension")
    public ResponseEntity<UserSuspensionResponseDto> suspendUser(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UserSuspensionRequest request
    ) {
        return ResponseEntity.ok(moderationService.suspendUser(userId, request));
    }

    @DeleteMapping("/follows/{followerId}/{followeeId}")
    public ResponseEntity<FollowModerationResponseDto> removeFollow(
            @PathVariable @Positive Long followerId,
            @PathVariable @Positive Long followeeId,
            @Valid @RequestBody FollowModerationRequest request
    ) {
        return ResponseEntity.ok(moderationService.removeFollow(followerId, followeeId, request));
    }
}
