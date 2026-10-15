package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.AiModerationFlagDto;
import com.metropolitan.VibeOn.dto.AiModerationReviewRequest;
import com.metropolitan.VibeOn.service.AiModerationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/moderation/ai-flags")
public class AiModerationController {
    private final AiModerationService aiModerationService;

    @GetMapping
    public ResponseEntity<List<AiModerationFlagDto>> getPendingFlags() {
        return ResponseEntity.ok(aiModerationService.getPendingFlags());
    }

    @PostMapping("/{flagId}/approve")
    public ResponseEntity<Void> approveFalsePositive(
            @PathVariable @Positive Long flagId,
            @Valid @RequestBody AiModerationReviewRequest request
    ) {
        aiModerationService.approveFalsePositive(flagId, request.reason());
        return ResponseEntity.noContent().build();
    }

    @Deprecated
    @PostMapping("/{flagId}/dismiss")
    public ResponseEntity<Void> dismissFlag(
            @PathVariable @Positive Long flagId,
            @Valid @RequestBody AiModerationReviewRequest request
    ) {
        return approveFalsePositive(flagId, request);
    }
}
