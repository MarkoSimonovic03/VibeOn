package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.entity.AiModerationCategory;
import com.metropolitan.VibeOn.service.PostModerationAnalysis;
import com.metropolitan.VibeOn.service.PostModerationAnalyzer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class LocalPostModerationAnalyzer implements PostModerationAnalyzer {
    private static final Map<AiModerationCategory, List<String>> TEXT_SIGNALS = Map.of(
            AiModerationCategory.NSFW, List.of("porn", "pornography", "nude", "nudity", "explicit sex", "sexual content"),
            AiModerationCategory.VIOLENCE_BLOOD, List.of("gore", "bloody", "bloodshed", "murder", "beheading", "graphic violence"),
            AiModerationCategory.WEAPONS, List.of("gun", "rifle", "pistol", "firearm", "knife", "weapon"),
            AiModerationCategory.DRUGS, List.of("cocaine", "heroin", "meth", "fentanyl", "illegal drugs", "drug dealing"),
            AiModerationCategory.HATE_HARASSMENT, List.of("kill yourself", "death threat", "racial slur", "hate speech", "harass"),
            AiModerationCategory.MENTION_ABUSE, List.of()
    );
    private static final Pattern MENTION_PATTERN = Pattern.compile(
            "(?<![\\p{L}\\p{N}_])@([\\p{L}\\p{N}_]+(?:[.-][\\p{L}\\p{N}_]+)*)"
    );

    private final double flagThreshold;

    public LocalPostModerationAnalyzer(
            @Value("${app.moderation.ai.flag-threshold:0.80}") double flagThreshold
    ) {
        this.flagThreshold = flagThreshold;
    }

    @Override
    public PostModerationAnalysis analyze(String text, List<MultipartFile> images) throws IOException {
        EnumMap<AiModerationCategory, Double> scores = new EnumMap<>(AiModerationCategory.class);
        for (AiModerationCategory category : AiModerationCategory.values()) {
            scores.put(category, textScore(text, category));
        }
        scores.put(AiModerationCategory.MENTION_ABUSE, mentionAbuseScore(
                text, scores.get(AiModerationCategory.HATE_HARASSMENT)
        ));
        double textConfidence = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);

        if (images != null) {
            for (MultipartFile image : images) {
                if (image == null || image.isEmpty()) continue;
                BufferedImage bufferedImage = ImageIO.read(image.getInputStream());
                if (bufferedImage != null) {
                    mergeImageScores(scores, analyzePixels(bufferedImage));
                }
            }
        }

        double highest = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        List<String> evidence = new ArrayList<>();
        scores.forEach((category, confidence) -> {
            if (confidence >= 0.45) {
                evidence.add(label(category) + " " + Math.round(confidence * 100) + "%");
            }
        });
        String summary = evidence.isEmpty()
                ? "No suspicious signals detected."
                : "Automated signals: " + String.join(", ", evidence) + ".";
        return new PostModerationAnalysis(highest >= flagThreshold, textConfidence, Map.copyOf(scores), summary);
    }

    private double textScore(String text, AiModerationCategory category) {
        if (text == null || text.isBlank()) return 0;
        String normalized = text.toLowerCase(Locale.ROOT);
        long matches = TEXT_SIGNALS.get(category).stream().filter(normalized::contains).count();
        if (matches == 0) return 0;
        return Math.min(0.99, 0.90 + (matches - 1) * 0.04);
    }

    private double mentionAbuseScore(String text, double harassmentConfidence) {
        if (text == null || text.isBlank()) return 0;
        Matcher matcher = MENTION_PATTERN.matcher(text);
        Map<String, Integer> occurrences = new HashMap<>();
        int total = 0;
        while (matcher.find()) {
            total++;
            occurrences.merge(matcher.group(1).toLowerCase(Locale.ROOT), 1, Integer::sum);
        }
        if (total == 0) return 0;
        if (harassmentConfidence >= flagThreshold) return 0.95;
        if (total >= 8) return 0.99;
        if (total >= 5) return 0.92;
        int repeated = occurrences.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return repeated >= 3 ? 0.90 : 0;
    }

    private Map<AiModerationCategory, Double> analyzePixels(BufferedImage image) {
        int stepX = Math.max(1, image.getWidth() / 128);
        int stepY = Math.max(1, image.getHeight() / 128);
        long sampled = 0;
        long skin = 0;
        long bloodRed = 0;
        long dark = 0;
        long saturatedDrugColors = 0;
        long sharpEdges = 0;

        for (int y = 0; y < image.getHeight(); y += stepY) {
            int previousBrightness = -1;
            for (int x = 0; x < image.getWidth(); x += stepX) {
                int rgb = image.getRGB(x, y);
                int red = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;
                int brightness = (red + green + blue) / 3;
                sampled++;
                if (red > 95 && green > 40 && blue > 20
                        && Math.max(red, Math.max(green, blue)) - Math.min(red, Math.min(green, blue)) > 15
                        && Math.abs(red - green) > 15 && red > green && red > blue) skin++;
                if (red > 140 && red > green * 1.5 && red > blue * 1.5) bloodRed++;
                if (brightness < 55) dark++;
                if ((green > 150 && green > red * 1.35 && green > blue * 1.15)
                        || (red > 120 && blue > 120 && green < 100)) saturatedDrugColors++;
                if (previousBrightness >= 0 && Math.abs(brightness - previousBrightness) > 100) sharpEdges++;
                previousBrightness = brightness;
            }
        }

        double total = Math.max(1, sampled);
        EnumMap<AiModerationCategory, Double> result = new EnumMap<>(AiModerationCategory.class);
        result.put(AiModerationCategory.NSFW, clamp((skin / total) * 1.25));
        result.put(AiModerationCategory.VIOLENCE_BLOOD, clamp((bloodRed / total) * 2.0));
        result.put(AiModerationCategory.WEAPONS, clamp((sharpEdges / total) * 2.4 + (dark / total) * 0.18));
        result.put(AiModerationCategory.DRUGS, clamp((saturatedDrugColors / total) * 1.3));
        result.put(AiModerationCategory.HATE_HARASSMENT, 0.0);
        result.put(AiModerationCategory.MENTION_ABUSE, 0.0);
        return result;
    }

    private void mergeImageScores(EnumMap<AiModerationCategory, Double> scores, Map<AiModerationCategory, Double> imageScores) {
        imageScores.forEach((category, confidence) -> scores.merge(category, confidence, Math::max));
    }

    private double clamp(double value) {
        return Math.max(0, Math.min(0.99, value));
    }

    private String label(AiModerationCategory category) {
        return category.name().toLowerCase(Locale.ROOT).replace('_', '/');
    }
}
