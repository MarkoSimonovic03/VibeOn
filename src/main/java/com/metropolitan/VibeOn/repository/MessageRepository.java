package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @EntityGraph(attributePaths = {"user", "sharedPost", "sharedPost.user"})
    List<Message> findByChatIdOrderByCreatedAtDesc(@Param("chatId") Long chatId);

    List<Message> findAllByChatIdAndUserIdNotAndReadFalse(Long chatId, Long userId);

    Optional<Message> findFirstByChatIdAndIdNotOrderByCreatedAtDesc(Long chatId, Long excludedMessageId);

    @Query("""
            SELECT COUNT(m)
            FROM Message m
            JOIN m.chat c
            JOIN c.users participant
            WHERE participant.id = :userId
              AND m.user.id <> :userId
              AND m.read = false
            """)
    long countUnreadMessagesForUser(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(m)
            FROM Message m
            WHERE m.chat.id = :chatId
              AND m.user.id <> :userId
              AND m.read = false
            """)
    long countUnreadMessagesForChatAndUser(
            @Param("chatId") Long chatId,
            @Param("userId") Long userId
    );

    default List<SingleMessageDto> findMessagesByChatIdAsDto(Long chatId) {
        return findByChatIdOrderByCreatedAtDesc(chatId).stream()
                .map(SingleMessageDto::fromMessage)
                .toList();
    }
}
