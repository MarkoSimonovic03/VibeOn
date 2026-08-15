package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.NotificationDto;
import com.metropolitan.VibeOn.dto.NotificationWsEventDto;
import com.metropolitan.VibeOn.entity.*;
import com.metropolitan.VibeOn.repository.NotificationRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.service.NotificationService;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private static final int MAX_CONTEXT_LENGTH = 500;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UserLookupService userLookupService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public List<NotificationDto> getCurrentUserNotifications() {
        return notificationRepository
                .findAllByRecipientIdAndTypeNotOrderByCreatedAtDescIdDesc(
                        userLookupService.getCurrentUser().getId(), NotificationType.CHAT_MESSAGE
                )
                .stream()
                .map(NotificationDto::fromNotification)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public long getUnreadCount() {
        return notificationRepository.countByRecipientIdAndTypeNotAndReadFalse(
                userLookupService.getCurrentUser().getId(), NotificationType.CHAT_MESSAGE
        );
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void markAsRead(Long notificationId) {
        User currentUser = userLookupService.getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NoSuchElementException("Notification not found."));
        if (!notification.getRecipient().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("This notification belongs to another user.");
        }
        notification.setRead(true);
        notificationRepository.flush();
        publish(notification.getRecipient(), "READ", NotificationDto.fromNotification(notification));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void markAllAsRead() {
        User currentUser = userLookupService.getCurrentUser();
        notificationRepository
                .findAllByRecipientIdAndTypeNotOrderByCreatedAtDescIdDesc(
                        currentUser.getId(), NotificationType.CHAT_MESSAGE
                )
                .forEach(notification -> notification.setRead(true));
        notificationRepository.flush();
        publish(currentUser, "READ_ALL", null);
    }

    @Override
    @Transactional
    public void notifyFollow(User actor, User recipient) {
        create(recipient, actor, NotificationType.FOLLOW, "Started following you", null, null);
    }

    @Override
    @Transactional
    public void notifyPostLike(User actor, Post post) {
        create(post.getUser(), actor, NotificationType.POST_LIKE,
                context("Liked your post", post.getDescription()), post.getId(), null);
    }

    @Override
    @Transactional
    public void notifyPostComment(User actor, Post post, String commentText) {
        create(post.getUser(), actor, NotificationType.POST_COMMENT,
                context("Commented on your post", commentText), post.getId(), null);
    }

    @Override
    @Transactional
    public void notifyPostMentions(User actor, Post post, List<String> usernames) {
        notifyMentions(actor, post, post.getDescription(), usernames, NotificationType.POST_MENTION);
    }

    @Override
    @Transactional
    public void notifyCommentMentions(User actor, Post post, String commentText, List<String> usernames) {
        notifyMentions(actor, post, commentText, usernames, NotificationType.COMMENT_MENTION);
    }

    private void notifyMentions(
            User actor,
            Post post,
            String text,
            List<String> usernames,
            NotificationType type
    ) {
        usernames.stream()
                .distinct()
                .map(userRepository::findByUsername)
                .flatMap(java.util.Optional::stream)
                .forEach(recipient -> create(
                        recipient,
                        actor,
                        type,
                        context(type == NotificationType.POST_MENTION
                                ? "Mentioned you in a post"
                                : "Mentioned you in a comment", text),
                        post.getId(),
                        null
                ));
    }

    private void create(
            User recipient,
            User actor,
            NotificationType type,
            String text,
            Long postId,
            Long chatId
    ) {
        if (recipient.getId().equals(actor.getId())) {
            return;
        }
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setActor(actor);
        notification.setType(type);
        notification.setContextText(limit(text));
        notification.setPostId(postId);
        notification.setChatId(chatId);
        notification.setCreatedAt(LocalDateTime.now());
        notification.setRead(false);
        Notification saved = notificationRepository.saveAndFlush(notification);
        publish(recipient, "UPSERT", NotificationDto.fromNotification(saved));
    }

    private void publish(User recipient, String action, NotificationDto notification) {
        messagingTemplate.convertAndSendToUser(
                recipient.getUsername(),
                "/queue/notifications",
                new NotificationWsEventDto(
                        action,
                        notification,
                        notificationRepository.countByRecipientIdAndTypeNotAndReadFalse(
                                recipient.getId(), NotificationType.CHAT_MESSAGE
                        )
                )
        );
    }

    private String context(String action, String detail) {
        return detail == null || detail.isBlank() ? action : action + ": " + detail.trim();
    }

    private String limit(String text) {
        return text.length() <= MAX_CONTEXT_LENGTH ? text : text.substring(0, MAX_CONTEXT_LENGTH);
    }
}
