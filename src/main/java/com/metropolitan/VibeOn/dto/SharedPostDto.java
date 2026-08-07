package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SharedPostDto {
    private Long id;
    private String imageUrl;
    private List<String> imageUrls;
    private String description;
    private String username;

    public static SharedPostDto fromPost(Post post) {
        if (post == null) {
            return null;
        }

        return new SharedPostDto(
                post.getId(),
                post.getImageUrl(),
                post.getEffectiveImageUrls(),
                post.getDescription(),
                post.getUser().getUsername()
        );
    }
}
