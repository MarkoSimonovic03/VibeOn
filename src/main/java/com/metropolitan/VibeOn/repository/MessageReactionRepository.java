package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.MessageReaction;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MessageReactionRepository extends JpaRepository<MessageReaction, Long> {
    Optional<MessageReaction> findByMessageIdAndUserId(Long messageId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<MessageReaction> findAllByMessageIdOrderByIdAsc(Long messageId);

    void deleteAllByMessageId(Long messageId);
}
