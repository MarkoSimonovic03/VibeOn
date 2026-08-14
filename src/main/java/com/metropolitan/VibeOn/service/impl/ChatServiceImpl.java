package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.dto.FullChatDto;
import com.metropolitan.VibeOn.dto.SingleChatDto;
import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.dto.ChatReadEventDto;
import com.metropolitan.VibeOn.dto.MessageReactionDto;
import com.metropolitan.VibeOn.dto.MessageReactionUpdateDto;
import com.metropolitan.VibeOn.entity.Chat;
import com.metropolitan.VibeOn.entity.Message;
import com.metropolitan.VibeOn.entity.MessageReaction;
import com.metropolitan.VibeOn.entity.Post;
import com.metropolitan.VibeOn.entity.User;
import com.metropolitan.VibeOn.repository.ChatRepository;
import com.metropolitan.VibeOn.repository.MessageRepository;
import com.metropolitan.VibeOn.repository.MessageReactionRepository;
import com.metropolitan.VibeOn.repository.PostRepository;
import com.metropolitan.VibeOn.service.ChatService;
import com.metropolitan.VibeOn.service.NotificationService;
import com.metropolitan.VibeOn.service.PostAccessService;
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
    private static final Set<String> ALLOWED_REACTIONS = Set.of("👍", "❤️", "😂", "😮", "😢");

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final MessageReactionRepository messageReactionRepository;
    private final PostRepository postRepository;
    private final UserLookupService userLookupService;
    private final PostAccessService postAccessService;
    private final NotificationService notificationService;

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
        message.setRead(false);

        Message savedMessage = messageRepository.save(message);
        chat.setLastMessage(savedMessage);
        notificationService.notifyChatMessage(currentUser, chat, savedMessage.getContent());

        return toMessageDto(savedMessage);
    }

    @Override
    @Transactional
    public SingleMessageDto sharePost(Long chatId, Long postId) {
        User currentUser = userLookupService.getCurrentUser();
        Chat chat = getAuthorizedChat(chatId, currentUser);
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found"));
        postAccessService.requireCanView(post, currentUser);

        Message message = new Message();
        message.setChat(chat);
        message.setUser(currentUser);
        message.setContent("Shared a post");
        message.setCreatedAt(LocalDateTime.now());
        message.setSharedPost(post);
        message.setRead(false);

        Message savedMessage = messageRepository.save(message);
        chat.setLastMessage(savedMessage);
        notificationService.notifyChatMessage(currentUser, chat, savedMessage.getContent());

        return toMessageDto(savedMessage);
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

        markIncomingMessagesRead(chat, currentUser);

        return new FullChatDto(
                chat.getId(),
                currentUser.getUsername(),
                currentUser.getProfileImageUrl(),
                otherUser.getUsername(),
                otherUser.getProfileImageUrl(),
                messageRepository.findByChatIdOrderByCreatedAtDesc(chat.getId()).stream()
                        .map(this::toMessageDto)
                        .toList()
        );
    }

    @Override
    @Transactional
    public ChatReadEventDto markChatAsRead(Long chatId) {
        User currentUser = userLookupService.getCurrentUser();
        Chat chat = getAuthorizedChat(chatId, currentUser);
        markIncomingMessagesRead(chat, currentUser);
        return new ChatReadEventDto(chatId, currentUser.getUsername());
    }

    @Override
    @Transactional
    public MessageReactionUpdateDto setReaction(Long chatId, Long messageId, String emoji) {
        User currentUser = userLookupService.getCurrentUser();
        Message message = getAuthorizedMessage(chatId, messageId, currentUser);
        if (!ALLOWED_REACTIONS.contains(emoji)) {
            throw new IllegalArgumentException("Unsupported reaction.");
        }

        MessageReaction reaction = messageReactionRepository
                .findByMessageIdAndUserId(messageId, currentUser.getId())
                .orElseGet(() -> {
                    MessageReaction created = new MessageReaction();
                    created.setMessage(message);
                    created.setUser(currentUser);
                    return created;
                });
        reaction.setEmoji(emoji);
        messageReactionRepository.save(reaction);
        return reactionUpdate(messageId);
    }

    @Override
    @Transactional
    public MessageReactionUpdateDto removeReaction(Long chatId, Long messageId) {
        User currentUser = userLookupService.getCurrentUser();
        getAuthorizedMessage(chatId, messageId, currentUser);
        messageReactionRepository.findByMessageIdAndUserId(messageId, currentUser.getId())
                .ifPresent(messageReactionRepository::delete);
        return reactionUpdate(messageId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireChatMembership(Long chatId, String username) {
        getAuthorizedChat(chatId, userLookupService.getByUsername(username));
    }

    private void markIncomingMessagesRead(Chat chat, User currentUser) {
        messageRepository.findAllByChatIdAndUserIdNotAndReadFalse(chat.getId(), currentUser.getId())
                .forEach(message -> message.setRead(true));
    }

    private Message getAuthorizedMessage(Long chatId, Long messageId, User currentUser) {
        getAuthorizedChat(chatId, currentUser);
        return messageRepository.findById(messageId)
                .filter(message -> message.getChat().getId().equals(chatId))
                .orElseThrow(() -> new NoSuchElementException("Message not found"));
    }

    private SingleMessageDto toMessageDto(Message message) {
        SingleMessageDto dto = SingleMessageDto.fromMessage(message);
        dto.setReactions(messageReactionRepository.findAllByMessageIdOrderByIdAsc(message.getId())
                .stream()
                .map(MessageReactionDto::fromReaction)
                .toList());
        return dto;
    }

    private MessageReactionUpdateDto reactionUpdate(Long messageId) {
        return new MessageReactionUpdateDto(
                messageId,
                messageReactionRepository.findAllByMessageIdOrderByIdAsc(messageId)
                        .stream()
                        .map(MessageReactionDto::fromReaction)
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
