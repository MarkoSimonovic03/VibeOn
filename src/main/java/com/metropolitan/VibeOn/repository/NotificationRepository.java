package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import com.metropolitan.VibeOn.entity.NotificationType;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByRecipientIdOrderByCreatedAtDescIdDesc(Long recipientId);

    long countByRecipientIdAndReadFalse(Long recipientId);

    List<Notification> findAllByRecipientIdAndTypeNotOrderByCreatedAtDescIdDesc(
            Long recipientId,
            NotificationType type
    );

    long countByRecipientIdAndTypeNotAndReadFalse(
            Long recipientId,
            NotificationType type
    );
}
