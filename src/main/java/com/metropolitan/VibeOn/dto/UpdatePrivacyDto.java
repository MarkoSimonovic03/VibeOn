package com.metropolitan.VibeOn.dto;

import jakarta.validation.constraints.NotNull;

public record UpdatePrivacyDto(@NotNull Boolean isPrivate) {
}
