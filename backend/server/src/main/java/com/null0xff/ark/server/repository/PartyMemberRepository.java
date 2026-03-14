package com.null0xff.ark.server.repository;

import com.null0xff.ark.server.entity.PartyMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link PartyMember} join-table entities.
 *
 * <p>Supports party roster lookups, character membership checks, and per-schedule
 * participation counts used to enforce the configurable party-slot limit.
 */
@Repository
public interface PartyMemberRepository extends JpaRepository<PartyMember, UUID> {

  /**
   * Returns all member records for the given party.
   *
   * @param partyId the party's unique identifier
   * @return a list of party memberships; empty if the party has no members
   */
  List<PartyMember> findByPartyId(UUID partyId);

  /**
   * Returns the membership record for a specific character in a specific party.
   *
   * @param partyId     the party's unique identifier
   * @param characterId the character's unique identifier
   * @return an {@link Optional} containing the record, or empty if the character has not joined
   */
  Optional<PartyMember> findByPartyIdAndCharacterId(UUID partyId, UUID characterId);

  /**
   * Returns the number of members currently in the given party.
   *
   * @param partyId the party's unique identifier
   * @return the member count
   */
  int countByPartyId(UUID partyId);

  /**
   * Returns the number of parties a character has joined within a specific schedule.
   *
   * <p>Used to enforce the per-schedule party-slot limit configured on the group.
   *
   * @param characterId the character's unique identifier
   * @param scheduleId  the schedule's unique identifier
   * @return the number of parties the character has joined in that schedule
   */
  int countByCharacterIdAndPartyScheduleId(UUID characterId, UUID scheduleId);
}
