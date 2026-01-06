package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.SinglePostDto;
import com.metropolitan.VibeOn.entity.Post;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;

public interface PostService {
    SinglePostDto createPost(MultipartFile image, String description) throws IOException;

    void softDeletePost(Long id) throws AccessDeniedException;

    SinglePostDto getPostById(Long id);

    SinglePostDto updatePost(Long id, String newDescription) throws AccessDeniedException;

    List<SinglePostDto> getPostsByUserId(Long userId);

    List<SinglePostDto> getAllPosts();

    List<SinglePostDto> getAllPostsByFolloweeUser();
}
