package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    @EntityGraph(attributePaths = {"user", "sharedPost", "sharedPost.user"})
    List<Message> findByChatIdOrderByCreatedAtDesc(@Param("chatId") Long chatId);

    List<Message> findAllByChatIdAndUserIdNotAndReadFalse(Long chatId, Long userId);

    default List<SingleMessageDto> findMessagesByChatIdAsDto(Long chatId) {
        return findByChatIdOrderByCreatedAtDesc(chatId).stream()
                .map(SingleMessageDto::fromMessage)
                .toList();
    }
}
