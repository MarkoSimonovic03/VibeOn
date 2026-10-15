package com.metropolitan.VibeOn.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ai_moderation_flags", uniqueConstraints = @UniqueConstraint(
        name = "uk_ai_moderation_flags_post", columnNames = "post_id"
))
public class AiModerationFlag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "post_id")
    private Post post;

    @OneToOne
    @JoinColumn(name = "message_id", unique = true)
    private Message message;

    @OneToOne
    @JoinColumn(name = "comment_id", unique = true)
    private PostComment comment;

    @OneToOne
    @JoinColumn(name = "story_id", unique = true)
    private Story story;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AiModerationFlagStatus status = AiModerationFlagStatus.PENDING;

    @Column(name = "text_confidence", nullable = false)
    private double textConfidence;

    @Column(name = "nsfw_confidence", nullable = false)
    private double nsfwConfidence;

    @Column(name = "violence_blood_confidence", nullable = false)
    private double violenceBloodConfidence;

    @Column(name = "weapons_confidence", nullable = false)
    private double weaponsConfidence;

    @Column(name = "drugs_confidence", nullable = false)
    private double drugsConfidence;

    @Column(name = "hate_harassment_confidence", nullable = false)
    private double hateHarassmentConfidence;

    @Column(name = "mention_abuse_confidence", nullable = false)
    private double mentionAbuseConfidence;

    @Column(length = 2000)
    private String summary;

    @Column(name = "flagged_at", nullable = false)
    private LocalDateTime flaggedAt;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;
}
