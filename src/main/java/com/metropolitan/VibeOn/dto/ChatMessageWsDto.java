package com.metropolitan.VibeOn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatMessageWsDto {
    @NotNull
    @Positive
    private Long chatId;

    @NotBlank
    @Size(max = 255)
    private String content;
}
