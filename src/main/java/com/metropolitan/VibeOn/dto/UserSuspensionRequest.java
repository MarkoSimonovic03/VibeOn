package com.metropolitan.VibeOn.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UserSuspensionRequest(
        @NotBlank @Size(max = 1000) String reason,
        @Min(1) @Max(720) int durationHours,
        @Positive Long reportId,
        @Positive Long aiFlagId
) {
}
