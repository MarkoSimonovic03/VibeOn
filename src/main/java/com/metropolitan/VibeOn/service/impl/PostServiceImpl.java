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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
}
