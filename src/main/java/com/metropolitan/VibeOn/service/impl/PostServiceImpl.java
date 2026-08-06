package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.PostLikeResponseDto;
import com.metropolitan.VibeOn.dto.SinglePostDto;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.PostLike;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.PostLikeRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.service.ImageStorageService;
import com.metropolitan.VibeOn.service.PostService;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final UserLookupService userLookupService;
    private final ImageStorageService imageStorageService;
    private final PostLikeRepository postLikeRepository;

    @Override
    @Transactional
    public SinglePostDto createPost(MultipartFile image, String description) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image is missing or empty");
        }

        User currentUser = userLookupService.getCurrentUser();

        Post post = new Post();
        post.setDescription(description);
        post.setUser(currentUser);
        post.setCreatedAt(LocalDateTime.now());
        post.setImageUrl(imageStorageService.save(image));

        Post savedPost = postRepository.save(post);

        return withLikeStatus(SinglePostDto.fromPost(savedPost), currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public SinglePostDto getPostById(Long id) {
        User currentUser = userLookupService.getCurrentUser();
        SinglePostDto post = postRepository.findPostByIdAsDto(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));
        return withLikeStatus(post, currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SinglePostDto> getAllPosts() {
        Long currentUserId = userLookupService.getCurrentUser().getId();
        return withLikeStatus(postRepository.findAllPostsAsDto(), currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SinglePostDto> getPostsByUsername(String username) {
        Long currentUserId = userLookupService.getCurrentUser().getId();
        return withLikeStatus(postRepository.findAllPostsByUsernameAsDto(username), currentUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SinglePostDto> getAllPostsByFolloweeUser() {
        Long currentUserId = userLookupService.getCurrentUser().getId();
        return withLikeStatus(postRepository.findAllPostsByFolloweeUserAsDto(currentUserId), currentUserId);
    }

    @Override
    @Transactional
    public SinglePostDto updatePost(Long id, String newDescription) throws AccessDeniedException {
        User currentUser = userLookupService.getCurrentUser();

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));

        if (!post.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have permission to update this post.");
        }

        post.setDescription(newDescription);
        postRepository.save(post);

        return withLikeStatus(SinglePostDto.fromPost(post), currentUser.getId());
    }

    @Override
    @Transactional
    public void softDeletePost(Long id) throws AccessDeniedException {
        User currentUser = userLookupService.getCurrentUser();

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));

        if (!post.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have permission to delete this post.");
        }

        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public PostLikeResponseDto likePost(Long id) {
        User currentUser = userLookupService.getCurrentUser();
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));

        if (!postLikeRepository.existsByPostIdAndUserId(id, currentUser.getId())) {
            PostLike postLike = new PostLike();
            postLike.setPost(post);
            postLike.setUser(currentUser);
            postLikeRepository.save(postLike);
        }

        return getLikeStatus(id, currentUser.getId());
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public PostLikeResponseDto unlikePost(Long id) {
        User currentUser = userLookupService.getCurrentUser();
        postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));

        postLikeRepository.findByPostIdAndUserId(id, currentUser.getId())
                .ifPresent(postLikeRepository::delete);

        return getLikeStatus(id, currentUser.getId());
    }

    private List<SinglePostDto> withLikeStatus(List<SinglePostDto> posts, Long currentUserId) {
        posts.forEach(post -> withLikeStatus(post, currentUserId));
        return posts;
    }

    private SinglePostDto withLikeStatus(SinglePostDto post, Long currentUserId) {
        post.setLikeCount(postLikeRepository.countByPostId(post.getId()));
        post.setLikedByCurrentUser(
                postLikeRepository.existsByPostIdAndUserId(post.getId(), currentUserId)
        );
        return post;
    }

    private PostLikeResponseDto getLikeStatus(Long postId, Long currentUserId) {
        return new PostLikeResponseDto(
                postId,
                postLikeRepository.countByPostId(postId),
                postLikeRepository.existsByPostIdAndUserId(postId, currentUserId)
        );
    }
}
