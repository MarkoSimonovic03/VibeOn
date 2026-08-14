package com.metropolitan.VibeOn.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChatTypingWsDto(@NotNull @Positive Long chatId, boolean typing) {
}
