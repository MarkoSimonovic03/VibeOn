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
@Table(name = "moderation_actions")
public class ModerationAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "moderator_id", nullable = false)
    private User moderator;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private ModerationTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "target_username", nullable = false)
    private String targetUsername;

    @Column(name = "secondary_target_id")
    private Long secondaryTargetId;

    @Column(name = "secondary_target_username")
    private String secondaryTargetUsername;

    @Column(name = "target_content", length = 4000)
    private String targetContent;

    @Column(name = "result_content", length = 4000)
    private String resultContent;

    @Column(name = "target_image_url")
    private String targetImageUrl;

    @Column(name = "target_context_id")
    private Long targetContextId;

    @Column(name = "target_created_at")
    private LocalDateTime targetCreatedAt;

    @Column(name = "reason", nullable = false, length = 1000)
    private String reason;

    @ManyToOne
    @JoinColumn(name = "report_id")
    private Report report;

    @ManyToOne
    @JoinColumn(name = "original_action_id")
    private ModerationAction originalAction;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
}
