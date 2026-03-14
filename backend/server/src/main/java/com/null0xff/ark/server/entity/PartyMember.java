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
 * Represents a character slot filled within a specific raid party.
 *
 * <p>Party membership is character-scoped: a user joins a party by selecting one of their
 * registered {@link GroupCharacter}s. The unique constraint prevents the same character
 * from occupying multiple slots in one party.
 */
@Entity
@Table(name = "party_members", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"party_id", "character_id"})
})
@Getter
@Setter
@NoArgsConstructor
public class PartyMember {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "party_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private Party party;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "character_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private GroupCharacter character;

  @Column(name = "joined_at", updatable = false)
  private Instant joinedAt;

  @PrePersist
  protected void onCreate() {
    this.joinedAt = Instant.now();
  }
}
