package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    @Query("SELECT c FROM Chat c JOIN c.users u WHERE u.id = :userId ORDER BY c.lastMessage.createdAt DESC")
    List<Chat> findAllByUserIdOrderByLastMessage(@Param("userId") Long userId);

    @Query("SELECT c FROM Chat c JOIN c.users u1 JOIN c.users u2 WHERE u1.id = :user1Id AND u2.id = :user2Id AND SIZE(c.users) = 2")
    Optional<Chat> findChatBetweenUsers(
            @Param("user1Id") Long user1Id,
            @Param("user2Id") Long user2Id
    );
}
