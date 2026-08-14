package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.PostCommentDto;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.PostComment;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.exception.PostCommentNotFoundException;
import com.metropolitan.VibeOn.repository.PostCommentRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.service.PostCommentService;
import com.metropolitan.VibeOn.service.MentionService;
import com.metropolitan.VibeOn.service.NotificationService;
import com.metropolitan.VibeOn.service.PostAccessService;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostCommentServiceImpl implements PostCommentService {
    private final PostCommentRepository postCommentRepository;
    private final PostRepository postRepository;
    private final UserLookupService userLookupService;
    private final MentionService mentionService;
    private final PostAccessService postAccessService;
    private final NotificationService notificationService;

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public List<PostCommentDto> getComments(Long postId) {
        requirePost(postId);
        return postCommentRepository.findAllByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public PostCommentDto addComment(Long postId, String content) {
        Post post = requirePost(postId);
        User currentUser = userLookupService.getCurrentUser();

        PostComment comment = new PostComment();
        comment.setPost(post);
        comment.setUser(currentUser);
        comment.setContent(content.trim());
        comment.setCreatedAt(LocalDateTime.now());

        PostComment savedComment = postCommentRepository.save(comment);
        notificationService.notifyPostComment(currentUser, post, savedComment.getContent());
        notificationService.notifyCommentMentions(
                currentUser,
                post,
                savedComment.getContent(),
                mentionService.findValidUsernames(savedComment.getContent())
        );
        return toDto(savedComment);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void deleteComment(Long postId, Long commentId) {
        requirePost(postId);
        User currentUser = userLookupService.getCurrentUser();
        PostComment comment = postCommentRepository.findById(commentId)
                .filter(existing -> existing.getPost().getId().equals(postId))
                .orElseThrow(PostCommentNotFoundException::new);

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("You cannot delete another user's comment.");
        }

        postCommentRepository.delete(comment);
    }

    private Post requirePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(PostCommentNotFoundException::new);
        postAccessService.requireCanView(post, userLookupService.getCurrentUser());
        return post;
    }

    private PostCommentDto toDto(PostComment comment) {
        PostCommentDto dto = PostCommentDto.fromComment(comment);
        dto.setMentionedUsernames(mentionService.findValidUsernames(comment.getContent()));
        return dto;
    }
}
