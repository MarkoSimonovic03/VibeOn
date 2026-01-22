package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.FullChatDto;
import com.metropolitan.VibeOn.dto.SingleChatDto;
import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.dto.SinglePostDto;
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

import java.nio.file.AccessDeniedException;
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

    @Override
    public SingleMessageDto sendMessage(String username, Long chatId, String content) {
        User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new RuntimeException("Chat not found"));

        boolean isMember = chat.getUsers().stream()
                .anyMatch(u -> u.getId().equals(currentUser.getId()));

        if (!isMember) {
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

        return SingleMessageDto.fromMessage(savedMessage);
    }

    public List<SingleChatDto> getChatsForCurrentUser() {
        return chatRepository.findAllChatsForUser(utilService.getCurrentUser().getId());
    }

    @Override
    public FullChatDto getChat(String username) {
        User currentUser = utilService.getCurrentUser();
        User otherUser = utilService.getUserByUsername(username);

        Chat chat = chatRepository
                .findChatBetweenUsers(currentUser.getId(), otherUser.getId())
                .orElseGet(() -> {
                    Chat newChat = new Chat();
                    newChat.setUsers(Set.of(currentUser, otherUser));
                    return chatRepository.save(newChat);
                });

        return new FullChatDto(
                chat.getId(),
                currentUser.getUsername(),
                currentUser.getProfileImageUrl(),
                otherUser.getUsername(),
                otherUser.getProfileImageUrl(),
                messageRepository.findMessagesByChatIdAsDto(chat.getId())
        );
    }
}

    //    public List<Chat> getChatsForCurrentUser() {
//        User currentUser = utilService.getCurrentUser();
//        return chatRepository.findAllByUserIdOrderByLastMessage(currentUser.getId());
//    }


//    @Override
//    public List<Message> getMessagesForChat(Long chatId) {
//        // Optional: proveri da li currentUser pripada chat-u
//        User currentUser = utilService.getCurrentUser();
//        Chat chat = chatRepository.findById(chatId)
//                .orElseThrow(() -> new RuntimeException("Chat not found"));
//
//        if (!chat.getUsers().contains(currentUser)) {
//            throw new RuntimeException("Not authorized for this chat");
//        }
//
//        return messageRepository.findByChatIdOrderByCreatedAtDesc(chatId);
//    }

//    @Override
//    public FullChatDto getMessagesForChat(Long chatId) throws AccessDeniedException {
//        User currentUser = utilService.getCurrentUser();
//
//        Chat chat = chatRepository.findById(chatId)
//                .orElseThrow(() -> new RuntimeException("Chat not found"));
//
//        if (!chat.getUsers().contains(currentUser)) {
//            throw new AccessDeniedException("Not authorized for this chat");
//        }
//
//        User otherUser = chat.getUsers().stream()
//                .filter(user -> !user.getId().equals(currentUser.getId()))
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("Other user not found"));
//
//        List<SingleMessageDto> messages =
//                messageRepository.findMessagesByChatIdAsDto(chatId);
//
//        return new FullChatDto(
//                chatId,
//                currentUser.getUsername(),
//                currentUser.getProfileImageUrl(),
//                otherUser.getUsername(),
//                otherUser.getProfileImageUrl(),
//                messages
//        );
//    }

