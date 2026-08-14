package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.FollowActionDto;
import com.metropolitan.VibeOn.dto.FollowRequestDto;
import com.metropolitan.VibeOn.dto.RelationshipUserDto;

import java.util.List;

public interface FollowService {
    FollowActionDto toggleFollow(Long userId);

    List<FollowRequestDto> getPendingRequests();

    void approveRequest(Long requestId);

    void rejectRequest(Long requestId);

    void cancelRequest(Long targetUserId);

    boolean updatePrivacy(boolean isPrivate);

    List<RelationshipUserDto> getFollowers(String username);

    List<RelationshipUserDto> getFollowing(String username);
}
