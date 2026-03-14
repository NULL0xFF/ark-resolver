package com.null0xff.ark.server.repository;

import com.null0xff.ark.server.entity.MemberAvailability;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link MemberAvailability} time-block entities.
 *
 * <p>Supports schedule-scoped availability aggregation and cascading deletion when a
 * member is removed from a group.
 */
@Repository
public interface MemberAvailabilityRepository extends JpaRepository<MemberAvailability, UUID> {

  /**
   * Returns all availability blocks submitted for the given schedule.
   *
   * @param scheduleId the schedule's unique identifier
   * @return a list of availability records across all users; empty if none submitted
   */
  List<MemberAvailability> findByScheduleId(UUID scheduleId);

  /**
   * Returns all availability blocks submitted by a specific user for the given schedule.
   *
   * @param scheduleId the schedule's unique identifier
   * @param userId     the user's unique identifier
   * @return a list of blocks; empty if the user has not submitted availability
   */
  List<MemberAvailability> findByScheduleIdAndUserId(UUID scheduleId, UUID userId);

  /**
   * Deletes all availability blocks submitted by a user for the given schedule.
   *
   * @param scheduleId the schedule's unique identifier
   * @param userId     the user's unique identifier
   */
  void deleteByScheduleIdAndUserId(UUID scheduleId, UUID userId);

  /**
   * Deletes all availability blocks belonging to a user across every schedule in the given group.
   *
   * <p>Used during member removal to clean up all of their data within a group in a single query.
   *
   * @param userId  the user's unique identifier
   * @param groupId the group's unique identifier
   */
  @Modifying
  @Query("DELETE FROM MemberAvailability ma WHERE ma.user.id = :userId AND ma.schedule.group.id = :groupId")
  void deleteByUserIdAndGroupId(UUID userId, UUID groupId);
}
