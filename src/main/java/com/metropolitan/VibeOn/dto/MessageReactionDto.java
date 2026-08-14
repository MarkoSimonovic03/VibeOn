package com.metropolitan.VibeOn.dto;

import com.metropolitan.VibeOn.entity.MessageReaction;

public record MessageReactionDto(String username, String emoji) {
    public static MessageReactionDto fromReaction(MessageReaction reaction) {
        return new MessageReactionDto(reaction.getUser().getUsername(), reaction.getEmoji());
    }
}
