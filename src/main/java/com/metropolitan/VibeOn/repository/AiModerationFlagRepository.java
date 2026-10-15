package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.AiModerationFlag;
import com.metropolitan.VibeOn.entity.AiModerationFlagStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiModerationFlagRepository extends JpaRepository<AiModerationFlag, Long> {
    @EntityGraph(attributePaths = {
            "post", "post.user", "post.imageUrls",
            "message", "message.user", "message.chat",
            "comment", "comment.user", "comment.post",
            "story", "story.user"
    })
    List<AiModerationFlag> findAllByStatusOrderByFlaggedAtDesc(AiModerationFlagStatus status);

    Optional<AiModerationFlag> findByPostId(Long postId);

    Optional<AiModerationFlag> findByMessageId(Long messageId);

    Optional<AiModerationFlag> findByCommentId(Long commentId);

    Optional<AiModerationFlag> findByStoryId(Long storyId);

    Optional<AiModerationFlag> findByIdAndStatus(Long id, AiModerationFlagStatus status);
}
