package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    public List<Post> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    public List<Post> findAllByOrderByCreatedAtDesc();
}
