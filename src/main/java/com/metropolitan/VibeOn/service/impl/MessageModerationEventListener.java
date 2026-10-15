package com.metropolitan.VibeOn.service.impl;

import com.metropolitan.VibeOn.entity.*;
import com.metropolitan.VibeOn.repository.AiModerationFlagRepository;
import com.metropolitan.VibeOn.repository.MessageRepository;
import com.metropolitan.VibeOn.service.MessageCreatedForModerationEvent;
import com.metropolitan.VibeOn.service.PostModerationAnalysis;
import com.metropolitan.VibeOn.service.PostModerationAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageModerationEventListener {
    private final MessageRepository messageRepository;
    private final AiModerationFlagRepository flagRepository;
    private final PostModerationAnalyzer analyzer;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void analyze(MessageCreatedForModerationEvent event) {
        try {
            Message message = messageRepository.findById(event.messageId()).orElse(null);
            if (message == null || message.getSharedPost() != null) return;
            PostModerationAnalysis analysis = analyzer.analyze(message.getContent(), List.of());
            if (!analysis.suspicious()) return;

            AiModerationFlag flag = new AiModerationFlag();
            flag.setMessage(message);
            flag.setTextConfidence(analysis.textConfidence());
            flag.setNsfwConfidence(analysis.confidence(AiModerationCategory.NSFW));
            flag.setViolenceBloodConfidence(analysis.confidence(AiModerationCategory.VIOLENCE_BLOOD));
            flag.setWeaponsConfidence(analysis.confidence(AiModerationCategory.WEAPONS));
            flag.setDrugsConfidence(analysis.confidence(AiModerationCategory.DRUGS));
            flag.setHateHarassmentConfidence(analysis.confidence(AiModerationCategory.HATE_HARASSMENT));
            flag.setMentionAbuseConfidence(analysis.confidence(AiModerationCategory.MENTION_ABUSE));
            flag.setSummary(analysis.summary());
            flag.setStatus(AiModerationFlagStatus.PENDING);
            flag.setFlaggedAt(LocalDateTime.now());
            flagRepository.save(flag);
        } catch (Exception exception) {
            log.warn("Background moderation failed for message {}; delivery is unaffected", event.messageId(), exception);
        }
    }
}
