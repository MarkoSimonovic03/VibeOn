package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.Notification;
import com.metropolitan.VibeOn.entity.NotificationType;

import java.time.LocalDateTime;

public record NotificationDto(
        Long id,
        NotificationType type,
        Long actorId,
        String actorUsername,
        String actorProfileImageUrl,
        String contextText,
        Long postId,
        Long chatId,
        LocalDateTime createdAt,
        boolean read
) {
    public static NotificationDto fromNotification(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType(),
                notification.getActor().getId(),
                notification.getActor().getUsername(),
                notification.getActor().getProfileImageUrl(),
                notification.getContextText(),
                notification.getPostId(),
                notification.getChatId(),
                notification.getCreatedAt(),
                notification.isRead()
        );
    }
}
