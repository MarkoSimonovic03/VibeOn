package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.FullChatDto;
import com.metropolitan.VibeOn.dto.SingleChatDto;
import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.entity.Chat;
import com.metropolitan.VibeOn.entity.Message;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.ChatRepository;
import com.metropolitan.VibeOn.repository.MessageRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.service.ChatService;
import com.metropolitan.VibeOn.service.UserLookupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final PostRepository postRepository;
    private final UserLookupService userLookupService;

    @Override
    @Transactional
    public SingleMessageDto sendMessage(String username, Long chatId, String content) {
        User currentUser = userLookupService.getByUsername(username);
        Chat chat = getAuthorizedChat(chatId, currentUser);

        Message message = new Message();
        message.setChat(chat);
        message.setUser(currentUser);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());

        Message savedMessage = messageRepository.save(message);
        chat.setLastMessage(savedMessage);

        return SingleMessageDto.fromMessage(savedMessage);
    }

    @Override
    @Transactional
    public SingleMessageDto sharePost(Long chatId, Long postId) {
        User currentUser = userLookupService.getCurrentUser();
        Chat chat = getAuthorizedChat(chatId, currentUser);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found"));

        Message message = new Message();
        message.setChat(chat);
        message.setUser(currentUser);
        message.setContent("Shared a post");
        message.setCreatedAt(LocalDateTime.now());
        message.setSharedPost(post);

        Message savedMessage = messageRepository.save(message);
        chat.setLastMessage(savedMessage);

        return SingleMessageDto.fromMessage(savedMessage);
    }

    @Override
    public List<SingleChatDto> getChatsForCurrentUser() {
        return chatRepository.findAllChatsForUser(userLookupService.getCurrentUser().getId());
    }

    @Override
    @Transactional
    public FullChatDto getChat(String username) {
        User currentUser = userLookupService.getCurrentUser();
        User otherUser = userLookupService.getByUsername(username);

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
                messageRepository.findByChatIdOrderByCreatedAtDesc(chat.getId()).stream()
                        .map(SingleMessageDto::fromMessage)
                        .toList()
        );
    }

    private Chat getAuthorizedChat(Long chatId, User currentUser) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new NoSuchElementException("Chat not found"));

        boolean isMember = chat.getUsers().stream()
                .anyMatch(user -> user.getId().equals(currentUser.getId()));
        if (!isMember) {
            throw new AccessDeniedException("Not authorized to send messages in this chat");
        }
        return chat;
    }
}
