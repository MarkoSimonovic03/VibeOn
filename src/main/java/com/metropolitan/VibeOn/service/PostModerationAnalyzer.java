package com.metropolitan.VibeOn.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface PostModerationAnalyzer {
    PostModerationAnalysis analyze(String text, List<MultipartFile> images) throws IOException;
}
