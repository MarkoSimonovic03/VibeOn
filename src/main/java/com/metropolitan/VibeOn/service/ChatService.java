package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.FullChatDto;
import com.metropolitan.VibeOn.dto.SingleChatDto;
import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.entity.Chat;
import com.metropolitan.VibeOn.entity.Message;

import java.nio.file.AccessDeniedException;
import java.util.List;

public interface ChatService {
    SingleMessageDto sendMessage(Long chatId, String content);

    List<SingleChatDto> getChatsForCurrentUser();

    FullChatDto getChat(String username);
}

//List<Chat> getChatsForCurrentUser();
//List<Message> getMessagesForChat(Long chatId);
//FullChatDto getMessagesForChat(Long chatId) throws AccessDeniedException;
