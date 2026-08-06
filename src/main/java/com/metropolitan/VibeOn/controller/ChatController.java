package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.service.ChatService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.NoSuchElementException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatController {
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

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

    @PostMapping("/{chatId}/posts/{postId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> sharePost(
            @PathVariable @Positive Long chatId,
            @PathVariable @Positive Long postId
    ) {
        try {
            SingleMessageDto message = chatService.sharePost(chatId, postId);
            messagingTemplate.convertAndSend("/topic/chat/" + chatId, message);
            return ResponseEntity.status(HttpStatus.CREATED).body(message);
        } catch (org.springframework.security.access.AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not authorized.");
        } catch (NoSuchElementException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Post or chat not found.");
        }
    }
}
