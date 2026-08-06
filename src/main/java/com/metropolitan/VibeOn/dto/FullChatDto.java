package com.metropolitan.VibeOn.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
