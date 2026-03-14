package com.null0xff.ark.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Response payload representing a schedule instance.
 *
 * <p>Status is derived at construction time by comparing the schedule's time window against
 * the current instant, so no separate status field needs to be persisted.
 */
@Data
@NoArgsConstructor
@Schema(description = "Response payload representing a schedule instance")
public class ScheduleResponse {

  @Schema(description = "Unique identifier of the schedule")
  private UUID id;

  @Schema(description = "Display title of the schedule")
  private String title;

  @Schema(description = "ISO-8601 start time", example = "2025-01-10T18:00:00Z")
  private String start;

  @Schema(description = "ISO-8601 end time", example = "2025-01-18T18:00:00Z")
  private String end;

  @Schema(description = "Derived status: PLANNED, ACTIVE, or PAST", example = "ACTIVE")
  private String status;

  @Schema(description = "Unique identifier of the group this schedule belongs to")
  private UUID groupId;

  /**
   * Constructs a {@code ScheduleResponse}, deriving {@link #status} from the schedule's
   * time window relative to the current instant.
   *
   * @param id    the schedule's unique identifier
   * @param title the schedule's display title
   * @param start the schedule start time; may be {@code null}
   * @param end   the schedule end time; may be {@code null}
   */
  public ScheduleResponse(UUID id, String title, Instant start, Instant end, UUID groupId) {
    this.id = id;
    this.title = title;
    this.start = start != null ? start.toString() : null;
    this.end = end != null ? end.toString() : null;
    this.groupId = groupId;

    if (start != null && end != null) {
      Instant now = Instant.now();
      if (now.isBefore(start)) {
        this.status = "PLANNED";
      } else if (now.isAfter(end)) {
        this.status = "PAST";
      } else {
        this.status = "ACTIVE";
      }
    }
  }
}
