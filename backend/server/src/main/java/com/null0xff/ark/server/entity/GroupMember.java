package com.null0xff.ark.server.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import com.null0xff.ark.server.enums.GroupRole;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Explicit join entity between User and Group to avoid implicit N:M relationships.
 * Stores membership details like roles and join dates.
 */
@Entity
@Table(name = "group_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_id", "user_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Group group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32, columnDefinition = "VARCHAR(32)")
    private GroupRole role;

    @Column(name = "joined_at", updatable = false)
    private Instant joinedAt;











}
