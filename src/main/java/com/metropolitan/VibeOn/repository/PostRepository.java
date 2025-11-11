package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findAllByUserIdOrderByCreatedAtDesc(Long userId);
    List<Post> findAllByOrderByCreatedAtDesc();

    @Query("SELECT p FROM Post p JOIN Follow f ON f.followee.id = p.user.id WHERE f.follower.id = :followerId ORDER BY p.createdAt DESC")
    List<Post> findAllPostsByFolloweeUser(@Param("followerId") Long followeeId);
}
