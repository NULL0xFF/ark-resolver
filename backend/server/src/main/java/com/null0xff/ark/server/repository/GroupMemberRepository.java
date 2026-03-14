package com.null0xff.ark.server.repository;

import com.null0xff.ark.server.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link GroupMember} join-table entities.
 *
 * <p>Provides lookup by user, by group, and by the composite (group, user) key used
 * throughout the authorization layer.
 */
@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

  /**
   * Returns all group memberships for the given user.
   *
   * @param userId the user's unique identifier
   * @return a list of memberships; empty if the user belongs to no groups
   */
  List<GroupMember> findByUserId(UUID userId);

  /**
   * Returns all members of the given group.
   *
   * @param groupId the group's unique identifier
   * @return a list of memberships; empty if the group has no members
   */
  List<GroupMember> findByGroupId(UUID groupId);

  /**
   * Returns the membership record for a specific user in a specific group.
   *
   * @param groupId the group's unique identifier
   * @param userId  the user's unique identifier
   * @return an {@link Optional} containing the record, or empty if the user is not a member
   */
  Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);
}
