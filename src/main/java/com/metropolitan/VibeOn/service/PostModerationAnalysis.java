package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.entity.AiModerationCategory;

import java.util.Map;

public record PostModerationAnalysis(
        boolean suspicious,
        double textConfidence,
        Map<AiModerationCategory, Double> categoryConfidences,
        String summary
) {
    public static PostModerationAnalysis safe() {
        return new PostModerationAnalysis(false, 0, Map.of(), "No suspicious signals detected.");
    }

    public double confidence(AiModerationCategory category) {
        return categoryConfidences.getOrDefault(category, 0.0);
    }
}
