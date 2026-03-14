package com.null0xff.ark.server.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a specific raid party organized within a schedule.
 */
@Entity
@Table(name = "parties")
@Getter
@Setter
@NoArgsConstructor
public class Party {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScheduleInstance schedule;

    @Column(nullable = false)
    private String title;

    @Column(name = "raid_type", nullable = false)
    private String raidType;

    @Column(name = "max_members", nullable = false)
    private Integer maxMembers;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "party", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PartyMember> members = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
