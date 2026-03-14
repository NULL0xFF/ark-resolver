package com.null0xff.ark.server.repository;

import com.null0xff.ark.server.entity.ScheduleInstance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link ScheduleInstance} entities.
 *
 * <p>Provides ordered retrieval of all schedules belonging to a group, which is the
 * primary access pattern for the schedule listing UI.
 */
@Repository
public interface ScheduleRepository extends JpaRepository<ScheduleInstance, UUID> {

  /**
   * Returns all non-archived schedules for the given group, ordered by start time ascending.
   *
   * @param groupId the group's unique identifier
   * @return a chronologically sorted list of active schedules; empty if none exist
   */
  List<ScheduleInstance> findByGroupIdAndArchivedFalseOrderByStartTimeAsc(UUID groupId);

  /**
   * Returns all archived schedules for the given group, ordered by start time descending.
   *
   * @param groupId the group's unique identifier
   * @return a reverse-chronological list of archived schedules; empty if none exist
   */
  List<ScheduleInstance> findByGroupIdAndArchivedTrueOrderByStartTimeDesc(UUID groupId);
}
