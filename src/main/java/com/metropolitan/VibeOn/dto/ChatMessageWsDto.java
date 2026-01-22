package com.metropolitan.VibeOn.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageWsDto {
    private Long chatId;
    private String content;
}
