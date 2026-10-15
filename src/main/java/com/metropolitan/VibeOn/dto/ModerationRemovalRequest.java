package com.metropolitan.VibeOn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ModerationRemovalRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Positive Long reportId,
        @Positive Long aiFlagId
) {
}
