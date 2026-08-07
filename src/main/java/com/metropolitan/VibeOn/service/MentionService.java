package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MentionService {
    private static final Pattern MENTION_PATTERN = Pattern.compile(
            "(?<![\\p{L}\\p{N}_])@([\\p{L}\\p{N}_]+(?:[.-][\\p{L}\\p{N}_]+)*)"
    );

    private final UserRepository userRepository;

    public List<String> findValidUsernames(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        Set<String> candidates = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            candidates.add(matcher.group(1));
        }

        return candidates.stream()
                .filter(userRepository::existsByUsername)
                .toList();
    }
}
