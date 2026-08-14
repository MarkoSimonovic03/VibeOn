package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.Message;
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
public class SingleMessageDto {
    private Long id;
    private String username;
    private String content;
    private LocalDateTime createdAt;
    private SharedPostDto sharedPost;
    private boolean read;
    private List<MessageReactionDto> reactions;

    public static SingleMessageDto fromMessage(Message message) {
        return new SingleMessageDto(
                message.getId(),
                message.getUser().getUsername(),
                message.getContent(),
                message.getCreatedAt(),
                SharedPostDto.fromPost(message.getSharedPost()),
                message.isRead(),
                List.of()
        );
    }
}
