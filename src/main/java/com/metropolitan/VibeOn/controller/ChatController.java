package com.metropolitan.VibeOn.controller;

import com.metropolitan.VibeOn.dto.FullChatDto;
import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.entity.Message;
import com.metropolitan.VibeOn.service.ChatService;
import com.metropolitan.VibeOn.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chats")
public class ChatController {
    private final ChatService chatService;



    @GetMapping()
    public ResponseEntity<?> getChatsForCurrentUser() {
        try {
            return ResponseEntity.ok(chatService.getChatsForCurrentUser());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    @GetMapping("/{username}")
    public ResponseEntity<?> getChat(@PathVariable String username) {
        try {
            return ResponseEntity.ok().body(chatService.getChat(username));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found" + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
        }
    }
}
//    @PostMapping("/messages/{chatId}")
//    public ResponseEntity<?> sendMessage(@PathVariable Long chatId,
//                                         @RequestBody Map<String, String> body) {
//        try {
//            SingleMessageDto message = chatService.sendMessage(chatId, body.get("content"));
//            return ResponseEntity.ok().body(message);
//        } catch (RuntimeException e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Chat not found or unauthorized: " + e.getMessage());
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred: " + e.getMessage());
//        }
//    }




//    // Endpoint za sve chatove trenutnog korisnika, sortirane po poslednjoj poruci
//    @GetMapping()
//    public ResponseEntity<List<Chat>> getChatsForCurrentUser() {
//        List<Chat> chats = chatService.getChatsForCurrentUser();
//        return ResponseEntity.ok(chats);
//    }

//    // Pregled svih poruka za dati chat
//    @GetMapping("/messages/{chatId}")
//    public ResponseEntity<?> getMessages(@PathVariable Long chatId) {
//        try {
//            List<Message> messages = chatService.getMessagesForChat(chatId);
//            return ResponseEntity.ok().body(messages);
//        } catch (RuntimeException e) {
//            return ResponseEntity.status(404).body("Chat not found: " + e.getMessage());
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
//        }
//    }

//    // Pregled svih poruka za dati chat
//    @GetMapping("/messages/{chatId}")
//    public ResponseEntity<?> getMessages(@PathVariable Long chatId) {
//        try {
//            FullChatDto chat = chatService.getMessagesForChat(chatId);
//            return ResponseEntity.ok().body(chat);
//        } catch (RuntimeException e) {
//            return ResponseEntity.status(404).body("Chat not found: " + e.getMessage());
//        } catch (Exception e) {
//            return ResponseEntity.status(500).body("An unexpected error occurred: " + e.getMessage());
//        }
//    }

