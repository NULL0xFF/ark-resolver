package com.null0xff.ark.server.repository;

import com.null0xff.ark.server.entity.InviteCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link InviteCode} entities.
 *
 * <p>Supports lookup by the human-readable code string and bulk retrieval by group,
 * both of which are required for invite validation and management UIs.
 */
@Repository
public interface InviteCodeRepository extends JpaRepository<InviteCode, UUID> {

  /**
   * Returns the invite code with the given code string.
   *
   * @param code the unique invite code string (e.g., {@code ARK-XXXX-XXXX})
   * @return an {@link Optional} containing the record, or empty if the code does not exist
   */
  Optional<InviteCode> findByCode(String code);

  /**
   * Returns all invite codes belonging to the given group.
   *
   * @param groupId the group's unique identifier
   * @return a list of invite codes; empty if none have been generated
   */
  List<InviteCode> findByGroupId(UUID groupId);
}
