package com.metropolitan.VibeOn.dto;

public record ChatUnreadWsEventDto(
        Long chatId,
        long unreadMessageCount
) {
}
