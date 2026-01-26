package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.entity.Follow;

public interface FollowService {
    void createFollow(Long userId);

    Boolean isFollowing(Long userId);

    void unFollow(Long userId);

    void toggleFollow(Long userId);
}
