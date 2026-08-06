package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.PostBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostBookmarkRepository extends JpaRepository<PostBookmark, Long> {
    boolean existsByPostIdAndUserId(Long postId, Long userId);

    Optional<PostBookmark> findByPostIdAndUserId(Long postId, Long userId);
}
