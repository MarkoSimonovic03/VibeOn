package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.SinglePostDto;
import com.metropolitan.VibeOn.dto.UserSearchResultDto;
import com.metropolitan.VibeOn.service.PostService;
import com.metropolitan.VibeOn.service.UserSearchService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('USER')")
@RequestMapping("/api/search")
public class SearchController {
    private final UserSearchService userSearchService;
    private final PostService postService;

    @GetMapping("/users")
    public ResponseEntity<List<UserSearchResultDto>> searchUsers(
            @RequestParam
            @NotBlank
            @Size(max = 50)
            String query
    ) {
        return ResponseEntity.ok(userSearchService.searchUsers(query));
    }

    @GetMapping("/posts")
    public ResponseEntity<List<SinglePostDto>> searchPosts(
            @RequestParam
            @NotBlank
            @Size(max = 50)
            @Pattern(regexp = "[\\p{L}\\p{N}_]+")
            String hashtag
    ) {
        return ResponseEntity.ok(postService.searchPostsByHashtag(hashtag));
    }
}
