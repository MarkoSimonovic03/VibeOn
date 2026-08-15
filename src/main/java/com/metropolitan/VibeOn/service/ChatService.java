package com.metropolitan.VibeOn.service;

import com.metropolitan.VibeOn.dto.FullChatDto;
import com.metropolitan.VibeOn.dto.SingleChatDto;
import com.metropolitan.VibeOn.dto.SingleMessageDto;
import com.metropolitan.VibeOn.dto.ChatReadEventDto;
import com.metropolitan.VibeOn.dto.MessageReactionUpdateDto;
import java.util.List;

public interface ChatService {
    SingleMessageDto sendMessage(String username, Long chatId, String content);

    SingleMessageDto sharePost(Long chatId, Long postId);

    List<SingleChatDto> getChatsForCurrentUser();

    long getUnreadMessageCount();

    FullChatDto getChat(String username);

    ChatReadEventDto markChatAsRead(Long chatId);

    MessageReactionUpdateDto setReaction(Long chatId, Long messageId, String emoji);

    MessageReactionUpdateDto removeReaction(Long chatId, Long messageId);

    void requireChatMembership(Long chatId, String username);
}
