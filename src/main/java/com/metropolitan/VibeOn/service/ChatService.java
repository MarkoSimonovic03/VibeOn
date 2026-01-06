package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.entity.Chat;
import com.metropolitan.VibeOn.entity.Message;

import java.util.List;

public interface ChatService {
    List<Chat> getChatsForCurrentUser();

    List<Message> getMessagesForChat(Long chatId);

    Message sendMessage(Long chatId, String content);

    Chat findOrCreateChat(String username);
}
