package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.FullChatDto;
import com.metropolitan.VibeOn.dto.SingleChatDto;
import com.metropolitan.VibeOn.dto.SingleMessageDto;
import java.util.List;

public interface ChatService {
    SingleMessageDto sendMessage(String username, Long chatId, String content);

    List<SingleChatDto> getChatsForCurrentUser();

    FullChatDto getChat(String username);
}
