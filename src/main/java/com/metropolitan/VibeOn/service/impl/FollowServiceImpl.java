package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.FollowActionDto;
import com.metropolitan.VibeOn.dto.FollowRequestDto;
import com.metropolitan.VibeOn.dto.RelationshipUserDto;
import com.metropolitan.VibeOn.entity.Follow;
import com.metropolitan.VibeOn.entity.FollowRequest;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.FollowRepository;
import com.metropolitan.VibeOn.repository.FollowRequestRepository;
import com.metropolitan.VibeOn.service.FollowService;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {
    private final FollowRepository followRepository;
    private final FollowRequestRepository followRequestRepository;
    private final UserLookupService userLookupService;

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public FollowActionDto toggleFollow(Long userId) {
        User follower = userLookupService.getCurrentUser();
        User followee = userLookupService.getById(userId);

        if (follower.getId().equals(followee.getId())) {
            throw new IllegalArgumentException("Users cannot follow themselves.");
        }

        Optional<Follow> existingFollow =
                followRepository.findByFollowerIdAndFolloweeId(follower.getId(), followee.getId());

        if (existingFollow.isPresent()) {
            followRepository.delete(existingFollow.get());
            return new FollowActionDto(false, false);
        }

        Optional<FollowRequest> existingRequest = followRequestRepository
                .findByRequesterIdAndTargetId(follower.getId(), followee.getId());

        if (followee.isPrivate()) {
            if (existingRequest.isEmpty()) {
                FollowRequest request = new FollowRequest();
                request.setRequester(follower);
                request.setTarget(followee);
                request.setCreatedAt(LocalDateTime.now());
                followRequestRepository.save(request);
            }
            return new FollowActionDto(false, true);
        }

        existingRequest.ifPresent(followRequestRepository::delete);
        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowee(followee);
        followRepository.save(follow);
        return new FollowActionDto(true, false);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public List<FollowRequestDto> getPendingRequests() {
        Long currentUserId = userLookupService.getCurrentUser().getId();
        return followRequestRepository.findAllByTargetIdOrderByCreatedAtAsc(currentUserId)
                .stream()
                .map(FollowRequestDto::fromRequest)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void approveRequest(Long requestId) {
        User currentUser = userLookupService.getCurrentUser();
        FollowRequest request = requireOwnedRequest(requestId, currentUser);
        if (!followRepository.existsByFollowerIdAndFolloweeId(
                request.getRequester().getId(), currentUser.getId()
        )) {
            Follow follow = new Follow();
            follow.setFollower(request.getRequester());
            follow.setFollowee(currentUser);
            followRepository.save(follow);
        }
        followRequestRepository.delete(request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void rejectRequest(Long requestId) {
        User currentUser = userLookupService.getCurrentUser();
        followRequestRepository.delete(requireOwnedRequest(requestId, currentUser));
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void cancelRequest(Long targetUserId) {
        User currentUser = userLookupService.getCurrentUser();
        FollowRequest request = followRequestRepository
                .findByRequesterIdAndTargetId(currentUser.getId(), targetUserId)
                .orElseThrow(() -> new NoSuchElementException("Follow request not found."));
        followRequestRepository.delete(request);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public boolean updatePrivacy(boolean isPrivate) {
        User currentUser = userLookupService.getCurrentUser();
        currentUser.setPrivate(isPrivate);
        return currentUser.isPrivate();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public List<RelationshipUserDto> getFollowers(String username) {
        User currentUser = userLookupService.getCurrentUser();
        User profileOwner = userLookupService.getByUsername(username);
        requireConnectionListAccess(currentUser, profileOwner);
        return followRepository.findAllByFolloweeIdOrderByFollowerUsernameAsc(profileOwner.getId())
                .stream()
                .map(follow -> relationshipUser(follow.getFollower(), currentUser))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public List<RelationshipUserDto> getFollowing(String username) {
        User currentUser = userLookupService.getCurrentUser();
        User profileOwner = userLookupService.getByUsername(username);
        requireConnectionListAccess(currentUser, profileOwner);
        return followRepository.findAllByFollowerIdOrderByFolloweeUsernameAsc(profileOwner.getId())
                .stream()
                .map(follow -> relationshipUser(follow.getFollowee(), currentUser))
                .toList();
    }

    private void requireConnectionListAccess(User currentUser, User profileOwner) {
        boolean isOwner = currentUser.getId().equals(profileOwner.getId());
        boolean approvedFollower = followRepository.existsByFollowerIdAndFolloweeId(
                currentUser.getId(), profileOwner.getId()
        );
        if (profileOwner.isPrivate() && !isOwner && !approvedFollower) {
            throw new AccessDeniedException("Followers and following are private.");
        }
    }

    private RelationshipUserDto relationshipUser(User user, User currentUser) {
        boolean isCurrentUser = currentUser.getId().equals(user.getId());
        return RelationshipUserDto.fromUser(
                user,
                !isCurrentUser && followRepository.existsByFollowerIdAndFolloweeId(
                        currentUser.getId(), user.getId()
                ),
                !isCurrentUser && followRequestRepository.existsByRequesterIdAndTargetId(
                        currentUser.getId(), user.getId()
                ),
                isCurrentUser
        );
    }

    private FollowRequest requireOwnedRequest(Long requestId, User currentUser) {
        FollowRequest request = followRequestRepository.findById(requestId)
                .orElseThrow(() -> new NoSuchElementException("Follow request not found."));
        if (!request.getTarget().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("This follow request belongs to another user.");
        }
        return request;
    }
}
