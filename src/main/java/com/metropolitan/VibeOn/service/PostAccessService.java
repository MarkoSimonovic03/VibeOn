package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.FollowRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PostAccessService {
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public boolean canViewAuthor(Long authorId, Long viewerId) {
        if (authorId.equals(viewerId)) {
            return true;
        }
        User author = userRepository.findById(authorId).orElse(null);
        return author != null && (
                !author.isPrivate()
                || followRepository.existsByFollowerIdAndFolloweeId(viewerId, authorId)
        );
    }

    public void requireCanView(Post post, User viewer) {
        if (!canViewAuthor(post.getUser().getId(), viewer.getId())) {
            throw new AccessDeniedException("This post belongs to a private account.");
        }
    }
}
