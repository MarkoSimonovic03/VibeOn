package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.dto.SinglePostDto;
import com.metropolitan.VibeOn.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("""
                SELECT new com.metropolitan.VibeOn.dto.SinglePostDto(
                    p.id,
                    p.imageUrl,
                    p.createdAt,
                    p.description,
                    u.id,
                    u.username,
                    u.profileImageUrl
                )
                FROM Post p
                JOIN p.user u
                WHERE p.id = :id
            """)
    Optional<SinglePostDto> findPostByIdAsDto(@Param("id") Long id);

    @Query("""
                SELECT new com.metropolitan.VibeOn.dto.SinglePostDto(
                    p.id,
                    p.imageUrl,
                    p.createdAt,
                    p.description,
                    u.id,
                    u.username,
                    u.profileImageUrl
                )
                FROM Post p
                JOIN p.user u
                ORDER BY p.createdAt DESC
            """)
    List<SinglePostDto> findAllPostsAsDto();

    @Query("""
                SELECT new com.metropolitan.VibeOn.dto.SinglePostDto(
                    p.id,
                    p.imageUrl,
                    p.createdAt,
                    p.description,
                    u.id,
                    u.username,
                    u.profileImageUrl
                )
                FROM Post p
                JOIN p.user u
                WHERE u.username = :username
                ORDER BY p.createdAt DESC
            """)
    List<SinglePostDto> findAllPostsByUsernameAsDto(@Param("username") String username);

    @Query("""
                SELECT new com.metropolitan.VibeOn.dto.SinglePostDto(
                    p.id,
                    p.imageUrl,
                    p.createdAt,
                    p.description,
                    u.id,
                    u.username,
                    u.profileImageUrl
                )
                FROM Post p
                JOIN p.user u
                JOIN Follow f ON f.followee.id = u.id
                WHERE f.follower.id = :followerId
                ORDER BY p.createdAt DESC
            """)
    List<SinglePostDto> findAllPostsByFolloweeUserAsDto(@Param("followerId") Long followerId);

    @Query("""
                SELECT new com.metropolitan.VibeOn.dto.SinglePostDto(
                    p.id,
                    p.imageUrl,
                    p.createdAt,
                    p.description,
                    u.id,
                    u.username,
                    u.profileImageUrl
                )
                FROM PostBookmark b
                JOIN b.post p
                JOIN p.user u
                WHERE b.user.id = :userId
                ORDER BY b.id DESC
            """)
    List<SinglePostDto> findAllBookmarkedPostsAsDto(@Param("userId") Long userId);

    @Query("""
                SELECT new com.metropolitan.VibeOn.dto.SinglePostDto(
                    p.id,
                    p.imageUrl,
                    p.createdAt,
                    p.description,
                    u.id,
                    u.username,
                    u.profileImageUrl
                )
                FROM Post p
                JOIN p.user u
                WHERE LOWER(p.description) LIKE LOWER(CONCAT('%#', :hashtag, '%'))
                ORDER BY p.createdAt DESC
            """)
    List<SinglePostDto> findHashtagCandidates(@Param("hashtag") String hashtag);

    @Query("""
                SELECT new com.metropolitan.VibeOn.dto.SinglePostDto(
                    p.id,
                    p.imageUrl,
                    p.createdAt,
                    p.description,
                    u.id,
                    u.username,
                    u.profileImageUrl
                )
                FROM Post p
                JOIN p.user u
                WHERE u.id != :currentUserId
                  AND NOT EXISTS (
                    SELECT f.id
                    FROM Follow f
                    WHERE f.follower.id = :currentUserId
                      AND f.followee.id = u.id
                  )
            """)
    List<SinglePostDto> findExploreCandidates(@Param("currentUserId") Long currentUserId);
}
