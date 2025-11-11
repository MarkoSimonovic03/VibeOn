package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.entity.Post;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;

public interface PostService {
    Post createPost(MultipartFile image, String description) throws IOException;

    void softDeletePost(Long id) throws AccessDeniedException;

    Post getPostById(Long id);

    Post updatePost(Long id, String newDescription) throws AccessDeniedException;

    List<Post> getPostsByUserId(Long userId);

    List<Post> getAllPosts();

    List<Post> getAllPostsByFolloweeUser();
}
