package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.StoryDto;
import com.metropolitan.VibeOn.service.StoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/stories")
public class StoryController {
    private final StoryService storyService;

    @PostMapping
    public ResponseEntity<StoryDto> createStory(
            @RequestPart("image") MultipartFile image
    ) throws IOException {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(storyService.createStory(image));
    }

    @GetMapping
    public ResponseEntity<List<StoryDto>> getActiveStories() {
        return ResponseEntity.ok(storyService.getActiveStories());
    }
}
