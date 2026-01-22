package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.ChatMessageWsDto;
import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.service.ChatService;
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

    /**
     * Frontend šalje poruku na:
     *   /app/chat.send
     *
     * Server emituje poruku na:
     *   /topic/chat/{chatId}
     */
    @MessageMapping("/chat.send")
    public void sendMessage(ChatMessageWsDto message, Principal principal) {

        if (principal == null) {
            throw new RuntimeException("User not authenticated");
        }

        // 1️⃣ Sačuvaj poruku sa korisnikom iz Principal-a
        SingleMessageDto savedMessage =
                chatService.sendMessage(principal.getName(), message.getChatId(), message.getContent());

        // 2️⃣ Pošalji realtime svim klijentima u tom chatu
        messagingTemplate.convertAndSend(
                "/topic/chat/" + message.getChatId(),
                savedMessage
        );
    }
}
