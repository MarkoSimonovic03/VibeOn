package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.dto.ChatReadEventDto;
import com.metropolitan.VibeOn.dto.MessageReactionUpdateDto;
import com.metropolitan.VibeOn.dto.UpdateMessageReactionDto;
import com.metropolitan.VibeOn.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
            var chat = chatService.getChat(username);
            messagingTemplate.convertAndSend(
                    "/topic/chat/" + chat.getId() + "/read",
                    new ChatReadEventDto(chat.getId(), chat.getUsername())
            );
            return ResponseEntity.ok(chat);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred.");
        }
    }

    @PatchMapping("/{chatId}/read")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> markChatAsRead(@PathVariable @Positive Long chatId) {
        ChatReadEventDto event = chatService.markChatAsRead(chatId);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/read", event);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{chatId}/messages/{messageId}/reaction")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageReactionUpdateDto> setReaction(
            @PathVariable @Positive Long chatId,
            @PathVariable @Positive Long messageId,
            @Valid @RequestBody UpdateMessageReactionDto request
    ) {
        MessageReactionUpdateDto update = chatService.setReaction(chatId, messageId, request.emoji());
        messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/reactions", update);
        return ResponseEntity.ok(update);
    }

    @DeleteMapping("/{chatId}/messages/{messageId}/reaction")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<MessageReactionUpdateDto> removeReaction(
            @PathVariable @Positive Long chatId,
            @PathVariable @Positive Long messageId
    ) {
        MessageReactionUpdateDto update = chatService.removeReaction(chatId, messageId);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/reactions", update);
        return ResponseEntity.ok(update);
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

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleMissingChatResource(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidChatRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
