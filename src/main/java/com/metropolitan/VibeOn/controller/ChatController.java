package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatController {
    private final ChatService chatService;

    @GetMapping
    public ResponseEntity<?> getChatsForCurrentUser() {
        try {
            return ResponseEntity.ok(chatService.getChatsForCurrentUser());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getChat(@PathVariable String username) {
        try {
            return ResponseEntity.ok(chatService.getChat(username));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }
}
