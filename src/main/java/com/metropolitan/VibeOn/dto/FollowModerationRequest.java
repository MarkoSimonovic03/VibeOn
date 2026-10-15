package com.metropolitan.VibeOn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FollowModerationRequest(@NotBlank @Size(max = 1000) String reason) {
}
