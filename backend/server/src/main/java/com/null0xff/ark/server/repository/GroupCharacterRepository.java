package com.null0xff.ark.server.repository;

import com.null0xff.ark.server.entity.GroupCharacter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link GroupCharacter} entities.
 *
 * <p>Provides character lookups scoped to a group and bulk deletion when a member
 * leaves or is removed from a group.
 */
@Repository
public interface GroupCharacterRepository extends JpaRepository<GroupCharacter, UUID> {

  /**
   * Returns all characters registered by a user in a specific group.
   *
   * @param groupId the group's unique identifier
   * @param userId  the user's unique identifier
   * @return a list of the user's characters in that group; empty if none registered
   */
  List<GroupCharacter> findByGroupIdAndUserId(UUID groupId, UUID userId);

  /**
   * Returns a specific character belonging to the given user in the given group.
   *
   * @param id      the character's unique identifier
   * @param groupId the group's unique identifier
   * @param userId  the user's unique identifier
   * @return an {@link Optional} containing the character, or empty if not found or not owned
   */
  Optional<GroupCharacter> findByIdAndGroupIdAndUserId(UUID id, UUID groupId, UUID userId);

  /**
   * Deletes all characters registered by a user in a specific group.
   *
   * <p>Used during member removal to clean up all character data. Associated
   * {@code PartyMember} rows are removed by the database-level cascade on the FK.
   *
   * @param userId  the user's unique identifier
   * @param groupId the group's unique identifier
   */
  @Modifying
  @Query("DELETE FROM GroupCharacter gc WHERE gc.user.id = :userId AND gc.group.id = :groupId")
  void deleteByUserIdAndGroupId(UUID userId, UUID groupId);
}
