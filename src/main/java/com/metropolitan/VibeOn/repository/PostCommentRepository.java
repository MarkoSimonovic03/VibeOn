package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.PostComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {
    List<PostComment> findAllByPostIdOrderByCreatedAtAsc(Long postId);

    long countByPostId(Long postId);
}
