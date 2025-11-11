package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    private static final String IMAGE_DIRECTORY = "images";

    // Method returns current user
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Method used to create new post
    public Post createPost(MultipartFile image, String description) throws IOException {
        Post post = new Post();
        post.setDescription(description);
        post.setUser(getCurrentUser());
        post.setCreatedAt(LocalDateTime.now());

        if (image != null && !image.isEmpty())
            post.setImageUrl(saveImage(image));
        else throw new IllegalArgumentException("Image error");

        return postRepository.save(post);
    }

    // Method used for saving images with unique name at images/...
    private String saveImage(MultipartFile image) throws IOException {
        File uploadDir = new File(IMAGE_DIRECTORY);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        // Generate unique name for the picture TODO fix name
        String imageName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        Path imagePath = Paths.get(IMAGE_DIRECTORY, imageName);

        Files.copy(image.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
        return imageName;
    }

    @Override
    public void softDeletePost(Long id) throws AccessDeniedException {
        User user = getCurrentUser();
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));
        if (post.getUser().equals(user)) {
            post.setDeletedAt(LocalDateTime.now());
            post.setDeleted(true);
            postRepository.save(post);
        } else
            throw new AccessDeniedException("You do not have permission to delete this post.");
    }

    @Override
    public Post getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));
        return post;
    }

    @Override
    public Post updatePost(Long id, String newDescription) throws AccessDeniedException {
        User user = getCurrentUser();
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found."));
        if (post.getUser().equals(user)) {
            post.setDescription(newDescription);
            return postRepository.save(post);
        } else
            throw new AccessDeniedException("You do not have permission to delete this post.");
    }

    @Override
    public List<Post> getPostsByUserId(Long userId) {
        return postRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Post> getAllPosts() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public List<Post> getAllPostsByFolloweeUser() {
        return postRepository.findAllPostsByFolloweeUser(getCurrentUser().getId());
    }
}
