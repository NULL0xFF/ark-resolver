package com.null0xff.ark.server.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a logical collective of users (Static, Guild, etc.) in the system.
 */
@Entity
@Table(name = "groups")
@Getter
@Setter
@NoArgsConstructor
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1024)
    private String description;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InviteCode> inviteCodes = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ScheduleInstance> schedules = new ArrayList<>();

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GroupCharacter> characters = new ArrayList<>();

    @Column(name = "max_parties_per_character", nullable = false)
    private Integer maxPartiesPerCharacter = 3;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
