package com.null0xff.ark.server.repository;

import com.null0xff.ark.server.entity.Party;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for {@link Party} entities.
 *
 * <p>Provides ordered retrieval of all parties belonging to a schedule, which is the
 * primary access pattern for the party listing UI.
 */
@Repository
public interface PartyRepository extends JpaRepository<Party, UUID> {

  /**
   * Returns all parties for the given schedule, ordered by start time ascending then title ascending.
   *
   * @param scheduleId the schedule's unique identifier
   * @return a sorted list of parties; empty if none have been created
   */
  List<Party> findByScheduleIdOrderByStartTimeAscTitleAsc(UUID scheduleId);
}
