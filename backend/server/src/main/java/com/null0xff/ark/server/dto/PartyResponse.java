package com.null0xff.ark.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response payload representing a raid party within a schedule.
 *
 * <p>Status is derived at construction time from the {@code isCompleted} flag and the
 * party's start time relative to the current instant, so no separate status field needs
 * to be persisted.
 */
@Data
@NoArgsConstructor
@Schema(description = "Response payload representing a raid party")
public class PartyResponse {

  @Schema(description = "Unique identifier of the party")
  private UUID id;

  @Schema(description = "Unique identifier of the schedule this party belongs to")
  private UUID scheduleId;

  @Schema(description = "Display title of the party")
  private String title;

  @Schema(description = "Raid or content type identifier")
  private String raidType;

  @Schema(description = "Current number of members in the party")
  private Integer members;

  @Schema(description = "Maximum number of members allowed")
  private Integer max;

  @Schema(description = "Derived status: Planned, On-going, or Done", example = "Planned")
  private String status;

  @Schema(description = "ISO-8601 start time of the party", example = "2025-01-15T18:00:00Z")
  private String start;

  @Schema(description = "Characters that have joined the party")
  private List<PartyMemberResponse> joinedMembers;

  /**
   * Constructs a {@code PartyResponse}, deriving {@link #status} from the completion flag
   * and the party's start time.
   *
   * @param id            the party's unique identifier
   * @param scheduleId    the unique identifier of the owning schedule
   * @param title         the party's display title
   * @param raidType      the raid or content type
   * @param members       current member count
   * @param max           maximum member capacity
   * @param start         the party start time; may be {@code null}
   * @param isCompleted   {@code true} if the party has been manually marked as done
   * @param joinedMembers characters currently occupying slots in this party
   */
  public PartyResponse(
      UUID id,
      UUID scheduleId,
      String title,
      String raidType,
      Integer members,
      Integer max,
      Instant start,
      Boolean isCompleted,
      List<PartyMemberResponse> joinedMembers) {
    this.id = id;
    this.scheduleId = scheduleId;
    this.title = title;
    this.raidType = raidType;
    this.members = members;
    this.max = max;
    this.start = start != null ? start.toString() : null;
    this.joinedMembers = joinedMembers;

    if (Boolean.TRUE.equals(isCompleted)) {
      this.status = "Done";
    } else if (start != null) {
      this.status = Instant.now().isAfter(start) ? "On-going" : "Planned";
    } else {
      this.status = "Planned";
    }
  }
}
