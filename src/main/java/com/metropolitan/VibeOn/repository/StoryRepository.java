package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.Story;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StoryRepository extends JpaRepository<Story, Long> {
    @EntityGraph(attributePaths = "user")
    List<Story> findAllByCreatedAtLessThanEqualAndExpiresAtAfterOrderByCreatedAtDesc(
            LocalDateTime createdAt,
            LocalDateTime expiresAt
    );
}
