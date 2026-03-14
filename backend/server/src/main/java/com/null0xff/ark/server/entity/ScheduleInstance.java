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
 * Represents a scheduled time block for a group, such as a weekly reset.
 */
@Entity
@Table(name = "schedules")
@Getter
@Setter
@NoArgsConstructor
public class ScheduleInstance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Group group;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private boolean archived = false;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MemberAvailability> availabilities = new ArrayList<>();

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Party> parties = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
