package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.entity.Chat;
import com.metropolitan.VibeOn.entity.Message;
import com.metropolitan.VibeOn.service.ChatService;
import com.metropolitan.VibeOn.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatController {
    private final MessageService messageService;
    private final ChatService chatService;

    @PostMapping("/find-or-create")
    public Chat findOrCreateChat(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        return chatService.findOrCreateChat(username);
    }

    // Endpoint za sve chatove trenutnog korisnika, sortirane po poslednjoj poruci
    @GetMapping()
    public ResponseEntity<List<Chat>> getChatsForCurrentUser() {
        List<Chat> chats = chatService.getChatsForCurrentUser();
        return ResponseEntity.ok(chats);
    }


    // Slanje poruke
    @PostMapping("/messages/{chatId}")
    public ResponseEntity<?> sendMessage(@PathVariable Long chatId,
                                         @RequestBody Map<String, String> body) {
        try {
            String content = body.get("content");
            Message message = chatService.sendMessage(chatId, content);
            return ResponseEntity.ok().body(message);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Chat not found or unauthorized: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }

    // Pregled svih poruka za dati chat
    @GetMapping("/messages/{chatId}")
    public ResponseEntity<?> getMessages(@PathVariable Long chatId) {
        try {
            List<Message> messages = chatService.getMessagesForChat(chatId);
            return ResponseEntity.ok().body(messages);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body("Chat not found: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
        }
    }
}
