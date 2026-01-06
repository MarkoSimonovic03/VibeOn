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
public class SingleChatDto {
    private Long id;
    private String otherUsername;
    private String otherProfileImageUrl;
    private String lastMessage;
    private LocalDateTime lastMessageCreatedAt;
    private String lastMessageUsername;
}