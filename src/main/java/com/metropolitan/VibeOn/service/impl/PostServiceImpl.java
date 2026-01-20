package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.ProfileDto;
import com.metropolitan.VibeOn.dto.SinglePostDto;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.service.PostService;
import com.metropolitan.VibeOn.service.util.UtilService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final UtilService utilService;

    @Override
    @Transactional
    public SinglePostDto createPost(MultipartFile image, String description) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Image is missing or empty");
        }

        User currentUser = utilService.getCurrentUser();

        Post post = new Post();
        post.setDescription(description);
        post.setUser(currentUser);
        post.setCreatedAt(LocalDateTime.now());
        post.setImageUrl(utilService.saveImage(image));

        Post savedPost = postRepository.save(post);

        return SinglePostDto.fromPost(savedPost);
    }

    @Override
    @Transactional(readOnly = true)
    public SinglePostDto getPostById(Long id) {
        return postRepository.findPostByIdAsDto(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SinglePostDto> getAllPosts() {
        return postRepository.findAllPostsAsDto();
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDto getPostsByUsername(String username) {
        User user = utilService.getUserByUsername(username);
        List<SinglePostDto> posts = postRepository.findAllPostsByUsernameAsDto(username);
        return ProfileDto.fromUserAndPosts(user,posts);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SinglePostDto> getAllPostsByFolloweeUser() {
        return postRepository.findAllPostsByFolloweeUserAsDto(utilService.getCurrentUser().getId());
    }

    @Override
    @Transactional
    public SinglePostDto updatePost(Long id, String newDescription) throws AccessDeniedException {
        User currentUser = utilService.getCurrentUser();

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));

        if (!post.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have permission to update this post.");
        }

        post.setDescription(newDescription);
        postRepository.save(post);

        return SinglePostDto.fromPost(post);
    }

    @Override
    @Transactional
    public void softDeletePost(Long id) throws AccessDeniedException {
        User currentUser = utilService.getCurrentUser();

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));

        if (!post.getUser().equals(currentUser)) {
            throw new AccessDeniedException("You do not have permission to delete this post.");
        }

        post.setDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
    }
}
