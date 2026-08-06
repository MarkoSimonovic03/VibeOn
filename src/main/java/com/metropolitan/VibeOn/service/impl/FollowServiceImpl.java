package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.entity.Follow;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.FollowRepository;
import com.metropolitan.VibeOn.service.FollowService;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
    private final FollowRepository followRepository;
    private final UserLookupService userLookupService;

    @Override
    public void toggleFollow(Long userId) {
        User follower = userLookupService.getCurrentUser();
        User followee = userLookupService.getById(userId);

        Optional<Follow> existingFollow =
                followRepository.findByFollowerIdAndFolloweeId(follower.getId(), followee.getId());

        if (existingFollow.isPresent()) {
            followRepository.delete(existingFollow.get());
            return;
        }

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowee(followee);
        followRepository.save(follow);
    }
}
