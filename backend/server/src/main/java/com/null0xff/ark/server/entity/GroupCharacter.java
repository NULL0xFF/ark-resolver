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
 * An in-game character registered by a user within a specific group.
 *
 * <p>Characters are group-scoped: the same user may register different characters in
 * different groups. Party slots are filled by characters, not by user accounts directly.
 */
@Entity
@Table(name = "group_characters", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"group_id", "user_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
public class GroupCharacter {

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

  @Column(nullable = false)
  private String name;

  @Column(name = "character_class")
  private String characterClass;

  @Column(name = "item_level")
  private Double itemLevel;

  @Column(name = "created_at", updatable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    this.createdAt = Instant.now();
  }
}
