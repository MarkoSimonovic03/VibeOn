package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.SingleChatDto;
import com.metropolitan.VibeOn.entity.Chat;
import com.metropolitan.VibeOn.entity.Message;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.ChatRepository;
import com.metropolitan.VibeOn.repository.MessageRepository;
import com.metropolitan.VibeOn.repository.UserRepository;
import com.metropolitan.VibeOn.service.ChatService;
import com.metropolitan.VibeOn.service.util.UtilService;
import jdk.jshell.execution.Util;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final UtilService utilService;

//    public List<Chat> getChatsForCurrentUser() {
//        User currentUser = utilService.getCurrentUser();
//        return chatRepository.findAllByUserIdOrderByLastMessage(currentUser.getId());
//    }

    public List<SingleChatDto> getChatsForCurrentUser() {
        return chatRepository.findAllChatsForUser(utilService.getCurrentUser().getId());
    }

    @Override
    public List<Message> getMessagesForChat(Long chatId) {
        // Optional: proveri da li currentUser pripada chat-u
        User currentUser = utilService.getCurrentUser();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.getUsers().contains(currentUser)) {
            throw new RuntimeException("Not authorized for this chat");
        }

        return messageRepository.findByChatIdOrderByCreatedAtDesc(chatId);
    }

    @Override
    public Message sendMessage(Long chatId, String content) {
        User currentUser = utilService.getCurrentUser();
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        if (!chat.getUsers().contains(currentUser)) {
            throw new RuntimeException("Not authorized to send message in this chat");
        }

        Message message = new Message();
        message.setChat(chat);
        message.setUser(currentUser);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);

        chat.setLastMessage(savedMessage);
        chatRepository.save(chat);

        return savedMessage;
    }

    @Override
    public Chat findOrCreateChat(String username) {
        User currentUser = utilService.getCurrentUser();

        User otherUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 1️⃣ Pokušaj da nađeš postojeći chat
        Optional<Chat> existingChat =
                chatRepository.findChatBetweenUsers(currentUser.getId(), otherUser.getId());

        if (existingChat.isPresent()) {
            return existingChat.get();
        }

        // 2️⃣ Ako ne postoji → kreiraj novi
        Chat chat = new Chat();
        chat.setUsers(Set.of(currentUser, otherUser));

        return chatRepository.save(chat);
    }
}
