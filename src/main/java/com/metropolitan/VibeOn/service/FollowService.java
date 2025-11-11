package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.entity.Follow;

public interface FollowService {
    Follow createFollow(Long userId);

    void unFollow(Long userId);
}
