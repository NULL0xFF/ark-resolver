package com.null0xff.ark.server.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents an invitation code used to join a group.
 */
@Entity
@Table(name = "invite_codes")
@Getter
@Setter
@NoArgsConstructor
public class InviteCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 32)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User createdBy;

    @Column(name = "max_usage")
    private Integer maxUsage;

    @Column(name = "current_usage", nullable = false)
    private Integer currentUsage = 0;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

















}
