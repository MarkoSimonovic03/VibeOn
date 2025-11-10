package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.entity.Post;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface PostService {
    Post createPost(MultipartFile image, String description) throws IOException;
}
