package com.metropolitan.VibeOn.dto;

import java.util.List;

public record MessageReactionUpdateDto(Long messageId, List<MessageReactionDto> reactions) {
}
