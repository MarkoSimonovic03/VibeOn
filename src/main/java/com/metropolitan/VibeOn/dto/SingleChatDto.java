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
    private long unreadCount;

    public SingleChatDto(
            Long id,
            String otherUsername,
            String otherProfileImageUrl,
            String lastMessage,
            LocalDateTime lastMessageCreatedAt,
            String lastMessageUsername
    ) {
        this.id = id;
        this.otherUsername = otherUsername;
        this.otherProfileImageUrl = otherProfileImageUrl;
        this.lastMessage = lastMessage;
        this.lastMessageCreatedAt = lastMessageCreatedAt;
        this.lastMessageUsername = lastMessageUsername;
    }
}
