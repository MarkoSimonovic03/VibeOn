package com.metropolitan.VibeOn.service.util;

import com.metropolitan.VibeOn.dto.SinglePostDto;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.UserRepository;
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

@Service
@RequiredArgsConstructor
public class UtilService {
    private final UserRepository userRepository;

    private static final String IMAGE_DIRECTORY = "images";

    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByUsername(auth.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public String saveImage(MultipartFile image) throws IOException {
        File uploadDir = new File(IMAGE_DIRECTORY);
        if (!uploadDir.exists()) uploadDir.mkdirs();

        String imageName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
        Path imagePath = Paths.get(IMAGE_DIRECTORY, imageName);

        Files.copy(image.getInputStream(), imagePath, StandardCopyOption.REPLACE_EXISTING);
        return imageName;
    }

    public SinglePostDto fromPostToSinglePostDto(Post post) {
        return new SinglePostDto(post.getId(),
                post.getImageUrl(),
                post.getCreatedAt(),
                post.getDescription(),
                post.getUser().getId(),
                post.getUser().getUsername(),
                post.getUser().getProfileImageUrl());
    }
}
