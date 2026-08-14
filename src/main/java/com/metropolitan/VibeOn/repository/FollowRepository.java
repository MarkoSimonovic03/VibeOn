package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    boolean existsByFollowerIdAndFolloweeId(Long followerId, Long followeeId);

    long countByFolloweeId(Long followeeId);

    long countByFollowerId(Long followerId);

    List<Follow> findAllByFolloweeIdOrderByFollowerUsernameAsc(Long followeeId);

    List<Follow> findAllByFollowerIdOrderByFolloweeUsernameAsc(Long followerId);
}
