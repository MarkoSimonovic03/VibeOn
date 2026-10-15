package com.metropolitan.VibeOn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiModerationReviewRequest(@NotBlank @Size(max = 1000) String reason) {
}
