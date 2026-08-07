package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.StoryDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface StoryService {
    StoryDto createStory(MultipartFile image) throws IOException;

    List<StoryDto> getActiveStories();
}
