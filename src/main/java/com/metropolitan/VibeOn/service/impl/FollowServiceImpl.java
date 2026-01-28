package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.entity.Follow;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.FollowRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.service.FollowService;
import com.metropolitan.VibeOn.service.util.UtilService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
    private final FollowRepository followRepository;
    private final UtilService utilService;

    @Override
    public void toggleFollow(Long userId) {
        User follower = utilService.getCurrentUser();
        User followee = utilService.getUserById(userId);

        Optional<Follow> followOpt = followRepository.findByFollowerIdAndFolloweeId(follower.getId(), followee.getId());

        if(followOpt.isPresent()){
            followRepository.delete(followOpt.get());
        }
        else{
            Follow follow = new Follow();
            follow.setFollower(follower);
            follow.setFollowee(followee);
            followRepository.save(follow);
        }
    }
}
