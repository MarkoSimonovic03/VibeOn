package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    //List<Message> findByChatIdOrderByCreatedAtDesc(Long chatId);

    @Query("""
                SELECT new com.metropolitan.VibeOn.dto.SingleMessageDto(
                    m.id,
                    u.username,
                    m.content,
                    m.createdAt
                )
                FROM Message m
                JOIN m.user u
                WHERE m.chat.id = :chatId
                ORDER BY m.createdAt DESC
            """)
    List<SingleMessageDto> findMessagesByChatIdAsDto(@Param("chatId") Long chatId);
}
