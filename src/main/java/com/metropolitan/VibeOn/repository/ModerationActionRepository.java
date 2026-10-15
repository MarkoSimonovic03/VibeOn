package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.ModerationAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface ModerationActionRepository extends JpaRepository<ModerationAction, Long> {
    @EntityGraph(attributePaths = {"moderator", "report", "originalAction"})
    List<ModerationAction> findAllByOrderByCreatedAtDesc();

    boolean existsByOriginalActionId(Long originalActionId);
}
