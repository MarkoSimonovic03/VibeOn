package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.PostLikeResponseDto;
import com.metropolitan.VibeOn.dto.SinglePostDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;

public interface PostService {
    SinglePostDto createPost(MultipartFile image, String description) throws IOException;

    void softDeletePost(Long id) throws AccessDeniedException;

    SinglePostDto getPostById(Long id);

    SinglePostDto updatePost(Long id, String newDescription) throws AccessDeniedException;

    List<SinglePostDto> getPostsByUsername(String username);

    List<SinglePostDto> getAllPosts();

    List<SinglePostDto> getAllPostsByFolloweeUser();

    PostLikeResponseDto likePost(Long id);

    PostLikeResponseDto unlikePost(Long id);
}
