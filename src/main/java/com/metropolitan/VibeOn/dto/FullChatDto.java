package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.Chat;
import com.metropolitan.VibeOn.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FullChatDto {
    private Long id;
    private String username;
    private String profileImageUrl;
    private String otherUsername;
    private String otherProfileImageUrl;
    private List<SingleMessageDto> messages;
}
