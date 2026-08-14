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
@Table(
        name = "follow_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_follow_requests_requester_target",
                columnNames = {"requester_id", "target_id"}
        )
)
public class FollowRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(optional = false)
    @JoinColumn(name = "target_id", nullable = false)
    private User target;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
