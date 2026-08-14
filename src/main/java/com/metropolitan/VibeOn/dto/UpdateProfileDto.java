package com.metropolitan.VibeOn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileDto(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String lastName,
        @Size(max = 500) String bio
) {
}
