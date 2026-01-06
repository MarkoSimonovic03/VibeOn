package com.metropolitan.VibeOn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SinglePostDto {
    private Long id;
    private String imageUrl;
    private LocalDateTime createdAt;
    private String description;
    private Long userId;
    private String username;
    private String profileImageUrl;
}
