package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.ChatMessageWsDto;
import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.dto.ChatTypingEventDto;
import com.metropolitan.VibeOn.dto.ChatTypingWsDto;
import com.metropolitan.VibeOn.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Valid ChatMessageWsDto message, Principal principal) {
        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }

        SingleMessageDto savedMessage = chatService.sendMessage(
                principal.getName(),
                message.getChatId(),
                message.getContent()
        );

        messagingTemplate.convertAndSend("/topic/chat/" + message.getChatId(), savedMessage);
    }

    @MessageMapping("/chat.typing")
    public void updateTyping(@Valid ChatTypingWsDto typing, Principal principal) {
        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }
        chatService.requireChatMembership(typing.chatId(), principal.getName());
        messagingTemplate.convertAndSend(
                "/topic/chat/" + typing.chatId() + "/typing",
                new ChatTypingEventDto(typing.chatId(), principal.getName(), typing.typing())
        );
    }
}
