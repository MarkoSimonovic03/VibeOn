package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.AiModerationFlagDto;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.PostComment;
import com.metropolitan.VibeOn.entity.Story;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface AiModerationService {
    void analyzeAndFlag(Post post, List<MultipartFile> images);

    void analyzeAndFlag(PostComment comment);

    void analyzeAndFlag(Story story, MultipartFile image);

    List<AiModerationFlagDto> getPendingFlags();

    void approveFalsePositive(Long flagId, String reason);
}
