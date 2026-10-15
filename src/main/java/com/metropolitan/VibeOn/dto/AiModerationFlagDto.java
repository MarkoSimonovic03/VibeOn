package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.AiModerationCategory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record AiModerationFlagDto(
        Long id,
        String targetType,
        Long postId,
        Long messageId,
        Long chatId,
        Long commentId,
        Long storyId,
        Long parentPostId,
        Long authorUserId,
        String authorUsername,
        String content,
        List<String> imageUrls,
        LocalDateTime targetCreatedAt,
        LocalDateTime flaggedAt,
        double textConfidence,
        Map<AiModerationCategory, Double> categoryConfidences,
        String summary
) {
}
