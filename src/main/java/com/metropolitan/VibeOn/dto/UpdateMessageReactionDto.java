package com.metropolitan.VibeOn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateMessageReactionDto(@NotBlank @Size(max = 10) String emoji) {
}
