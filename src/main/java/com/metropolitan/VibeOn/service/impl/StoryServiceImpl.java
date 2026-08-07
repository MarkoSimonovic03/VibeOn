package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.StoryDto;
import com.metropolitan.VibeOn.entity.Story;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.StoryRepository;
import com.metropolitan.VibeOn.service.ImageStorageService;
import com.metropolitan.VibeOn.service.StoryService;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StoryServiceImpl implements StoryService {
    private static final int STORY_LIFETIME_HOURS = 24;

    private final StoryRepository storyRepository;
    private final UserLookupService userLookupService;
    private final ImageStorageService imageStorageService;

    @Override
    @Transactional
    @PreAuthorize("hasRole('USER')")
    public StoryDto createStory(MultipartFile image) throws IOException {
        User currentUser = userLookupService.getCurrentUser();
        LocalDateTime createdAt = LocalDateTime.now();

        Story story = new Story();
        story.setImageUrl(imageStorageService.save(image));
        story.setCreatedAt(createdAt);
        story.setExpiresAt(createdAt.plusHours(STORY_LIFETIME_HOURS));
        story.setUser(currentUser);

        return StoryDto.fromStory(storyRepository.save(story), currentUser.getId());
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('USER')")
    public List<StoryDto> getActiveStories() {
        User currentUser = userLookupService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();

        return storyRepository
                .findAllByCreatedAtLessThanEqualAndExpiresAtAfterOrderByCreatedAtDesc(now, now)
                .stream()
                .map(story -> StoryDto.fromStory(story, currentUser.getId()))
                .toList();
    }
}
