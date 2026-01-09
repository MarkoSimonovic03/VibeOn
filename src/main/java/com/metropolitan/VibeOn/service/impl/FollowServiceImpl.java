package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.entity.Follow;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.FollowRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.service.FollowService;
import com.metropolitan.VibeOn.service.util.UtilService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final UtilService utilService;

    @Override
    public void createFollow(Long userId) {
        User follower = utilService.getCurrentUser();
        User followee = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Followee not found"));

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowee(followee);
        followRepository.save(follow);
    }

    @Override
    public Boolean isFollowing(Long userId) {
        User currentUser = utilService.getCurrentUser();
        User otherUser = utilService.getUserById(userId);

        Optional<Follow> follow = followRepository.findByFollowerIdAndFolloweeId(currentUser.getId(),otherUser.getId());

        return follow.isPresent();
    }


    @Override
    public void unFollow(Long userId) {
        User follower = utilService.getCurrentUser();
        User followee = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Followee not found"));

        Follow follow = followRepository.findByFollowerIdAndFolloweeId(follower.getId(), followee.getId())
                .orElseThrow(() -> new RuntimeException("Follow not found"));

        followRepository.delete(follow);
    }


}
