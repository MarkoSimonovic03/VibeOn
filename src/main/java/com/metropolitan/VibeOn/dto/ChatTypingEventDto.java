package com.metropolitan.VibeOn.dto;

public record ChatTypingEventDto(Long chatId, String username, boolean typing) {
}
