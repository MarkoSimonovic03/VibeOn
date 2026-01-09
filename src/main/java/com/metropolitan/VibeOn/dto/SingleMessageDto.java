package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.Message;
import com.metropolitan.VibeOn.entity.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SingleMessageDto {
    private Long id;
    private String username;
    private String content;
    private LocalDateTime createdAt;

    public static SingleMessageDto fromMessage(Message message) {
        return new SingleMessageDto(
                message.getId(),
                message.getUser().getUsername(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
