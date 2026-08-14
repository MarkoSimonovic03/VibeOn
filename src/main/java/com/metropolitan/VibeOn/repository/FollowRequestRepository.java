package com.metropolitan.VibeOn.repository;

import com.metropolitan.VibeOn.entity.FollowRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRequestRepository extends JpaRepository<FollowRequest, Long> {
    Optional<FollowRequest> findByRequesterIdAndTargetId(Long requesterId, Long targetId);

    boolean existsByRequesterIdAndTargetId(Long requesterId, Long targetId);

    List<FollowRequest> findAllByTargetIdOrderByCreatedAtAsc(Long targetId);
}
