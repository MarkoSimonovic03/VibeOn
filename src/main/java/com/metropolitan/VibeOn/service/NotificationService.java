package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.NotificationDto;
import com.metropolitan.VibeOn.entity.Chat;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.User;

import java.util.List;

public interface NotificationService {
    List<NotificationDto> getCurrentUserNotifications();

    long getUnreadCount();

    void markAsRead(Long notificationId);

    void markAllAsRead();

    void notifyFollow(User actor, User recipient);

    void notifyPostLike(User actor, Post post);

    void notifyPostComment(User actor, Post post, String commentText);

    void notifyChatMessage(User actor, Chat chat, String messageText);

    void notifyPostMentions(User actor, Post post, List<String> usernames);

    void notifyCommentMentions(User actor, Post post, String commentText, List<String> usernames);
}
