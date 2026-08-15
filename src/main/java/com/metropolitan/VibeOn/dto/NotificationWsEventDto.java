package com.metropolitan.VibeOn.dto;

public record NotificationWsEventDto(
        String action,
        NotificationDto notification,
        long unreadCount
) {
}
