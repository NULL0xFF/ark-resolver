package com.null0xff.ark.server.controller;

import com.null0xff.ark.server.dto.AvailabilityRequest;
import com.null0xff.ark.server.dto.AvailabilityResponse;
import com.null0xff.ark.server.dto.PartyRequest;
import com.null0xff.ark.server.dto.PartyResponse;
import com.null0xff.ark.server.dto.ScheduleRequest;
import com.null0xff.ark.server.dto.ScheduleResponse;
import com.null0xff.ark.server.service.AvailabilityService;
import com.null0xff.ark.server.service.PartyService;
import com.null0xff.ark.server.service.ScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for schedule CRUD, member availability, and party listing within schedules.
 *
 * <p>Schedule creation/modification requires {@code MANAGER} or {@code AUDITOR} role.
 * Availability submission is open to all group members. Party creation requires
 * {@code MANAGER} or {@code AUDITOR} role.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Schedules & Availability", description = "Endpoints for managing schedules, availability, and parties")
public class ScheduleController {

  private final ScheduleService scheduleService;
  private final AvailabilityService availabilityService;
  private final PartyService partyService;

  /**
   * Creates a new schedule for the group. Requires {@code MANAGER} or {@code AUDITOR} role.
   *
   * @param jwt     the caller's JWT
   * @param request the schedule request
   * @return the created schedule
   */
  @Operation(summary = "Create Schedule", description = "Creates a new schedule for the group. Requires MANAGER or AUDITOR role.")
  @ApiResponse(responseCode = "200", description = "Schedule created successfully")
  @ApiResponse(responseCode = "400", description = "Invalid time range (end must be after start)")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller lacks MANAGER or AUDITOR role")
  @PostMapping("/api/schedules/create")
  public ResponseEntity<ScheduleResponse> createSchedule(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody ScheduleRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(
        scheduleService.createSchedule(request.getGroupId(), userId, request.getTitle(), request.getStartTime(), request.getEndTime()));
  }

  /**
   * Returns all schedules for the given group, ordered by start time. Requires membership.
   *
   * @param jwt     the caller's JWT
   * @param groupId the group's unique identifier
   * @return a chronologically sorted list of schedules
   */
  @Operation(summary = "List Schedules", description = "Returns all schedules for the group, ordered by start time. Requires membership.")
  @ApiResponse(responseCode = "200", description = "Schedules retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller is not a member of the group")
  @GetMapping("/api/schedules/list")
  public ResponseEntity<List<ScheduleResponse>> getGroupSchedules(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam UUID groupId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(scheduleService.getGroupSchedules(groupId, userId));
  }

  /**
   * Retrieves details of a specific schedule. Requires membership in the owning group.
   *
   * @param jwt        the caller's JWT
   * @param scheduleId the schedule's unique identifier
   * @return the schedule details
   */
  @Operation(summary = "Get Schedule Details", description = "Retrieves details of a specific schedule. Requires membership in the owning group.")
  @ApiResponse(responseCode = "200", description = "Schedule retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller is not a member of the owning group")
  @ApiResponse(responseCode = "404", description = "Schedule not found")
  @GetMapping("/api/schedules/detail")
  public ResponseEntity<ScheduleResponse> getSchedule(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam UUID scheduleId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(scheduleService.getSchedule(scheduleId, userId));
  }

  /**
   * Updates a schedule's title and time window. Requires {@code MANAGER} or {@code AUDITOR} role.
   *
   * @param jwt        the caller's JWT
   * @param request    the schedule request
   * @return 200 OK on success
   */
  @Operation(summary = "Update Schedule", description = "Updates a schedule's title and time window. Requires MANAGER or AUDITOR role.")
  @ApiResponse(responseCode = "200", description = "Schedule updated successfully")
  @ApiResponse(responseCode = "400", description = "Invalid time range")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller lacks MANAGER or AUDITOR role")
  @ApiResponse(responseCode = "404", description = "Schedule not found")
  @PostMapping("/api/schedules/update")
  public ResponseEntity<Void> updateSchedule(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody ScheduleRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    scheduleService.updateSchedule(request.getScheduleId(), userId, request.getTitle(), request.getStartTime(), request.getEndTime());
    return ResponseEntity.ok().build();
  }

  /**
   * Permanently deletes a schedule and all associated data. Requires {@code MANAGER} or
   * {@code AUDITOR} role.
   *
   * @param jwt        the caller's JWT
   * @param payload    the request containing scheduleId
   * @return 200 OK on success
   */
  @Operation(summary = "Delete Schedule", description = "Permanently deletes a schedule and all associated data. Requires MANAGER or AUDITOR role.")
  @ApiResponse(responseCode = "200", description = "Schedule deleted successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller lacks MANAGER or AUDITOR role")
  @ApiResponse(responseCode = "404", description = "Schedule not found")
  @PostMapping("/api/schedules/delete")
  public ResponseEntity<Void> deleteSchedule(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody Map<String, UUID> payload) {
    UUID userId = UUID.fromString(jwt.getSubject());
    UUID scheduleId = payload.get("scheduleId");
    scheduleService.deleteSchedule(scheduleId, userId);
    return ResponseEntity.ok().build();
  }

  /**
   * Archives a schedule, hiding it from the active schedule list. Requires {@code MANAGER} or
   * {@code AUDITOR} role.
   *
   * @param jwt     the caller's JWT
   * @param payload the request containing scheduleId
   * @return 200 OK on success
   */
  @Operation(summary = "Archive Schedule", description = "Archives a schedule. Archived schedules are excluded from the main list but remain accessible via history. Requires MANAGER or AUDITOR role.")
  @ApiResponse(responseCode = "200", description = "Schedule archived successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller lacks MANAGER or AUDITOR role")
  @ApiResponse(responseCode = "404", description = "Schedule not found")
  @PostMapping("/api/schedules/archive")
  public ResponseEntity<Void> archiveSchedule(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody Map<String, UUID> payload) {
    UUID userId = UUID.fromString(jwt.getSubject());
    UUID scheduleId = payload.get("scheduleId");
    scheduleService.archiveSchedule(scheduleId, userId);
    return ResponseEntity.ok().build();
  }

  /**
   * Restores an archived schedule back to the active list. Requires {@code MANAGER} or
   * {@code AUDITOR} role.
   *
   * @param jwt     the caller's JWT
   * @param payload the request containing scheduleId
   * @return 200 OK on success
   */
  @Operation(summary = "Unarchive Schedule", description = "Restores an archived schedule to the active list. Requires MANAGER or AUDITOR role.")
  @ApiResponse(responseCode = "200", description = "Schedule unarchived successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller lacks MANAGER or AUDITOR role")
  @ApiResponse(responseCode = "404", description = "Schedule not found")
  @PostMapping("/api/schedules/unarchive")
  public ResponseEntity<Void> unarchiveSchedule(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody Map<String, UUID> payload) {
    UUID userId = UUID.fromString(jwt.getSubject());
    UUID scheduleId = payload.get("scheduleId");
    scheduleService.unarchiveSchedule(scheduleId, userId);
    return ResponseEntity.ok().build();
  }

  /**
   * Returns all archived schedules for the given group. Requires membership.
   *
   * @param jwt     the caller's JWT
   * @param groupId the group's unique identifier
   * @return a reverse-chronological list of archived schedules
   */
  @Operation(summary = "List Archived Schedules", description = "Returns all archived schedules for the group, ordered by start time descending. Requires membership.")
  @ApiResponse(responseCode = "200", description = "Archived schedules retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller is not a member of the group")
  @GetMapping("/api/schedules/archived")
  public ResponseEntity<List<ScheduleResponse>> getArchivedSchedules(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam UUID groupId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(scheduleService.getArchivedSchedules(groupId, userId));
  }

  /**
   * Overwrites the caller's availability for the given schedule. Requires membership.
   *
   * @param jwt        the caller's JWT
   * @param request    the availability request
   * @return 200 OK on success
   */
  @Operation(summary = "Submit My Availability", description = "Overwrites the caller's availability for a schedule. Requires membership in the owning group.")
  @ApiResponse(responseCode = "200", description = "Availability saved successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller is not a member of the owning group")
  @ApiResponse(responseCode = "404", description = "Schedule not found")
  @PostMapping("/api/schedules/availability/me")
  public ResponseEntity<Void> updateMyAvailability(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody AvailabilityRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    availabilityService.updateAvailability(request.getScheduleId(), userId, request.getBlocks());
    return ResponseEntity.ok().build();
  }

  /**
   * Returns all members' availability for the given schedule. Requires membership.
   *
   * @param jwt        the caller's JWT
   * @param scheduleId the schedule's unique identifier
   * @return a list of per-user availability block collections
   */
  @Operation(summary = "Get Aggregated Availability", description = "Returns all members' availability for a schedule. Requires membership.")
  @ApiResponse(responseCode = "200", description = "Availability retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller is not a member of the owning group")
  @ApiResponse(responseCode = "404", description = "Schedule not found")
  @GetMapping("/api/schedules/availability")
  public ResponseEntity<List<AvailabilityResponse>> getAggregatedAvailability(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam UUID scheduleId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(availabilityService.getAggregatedAvailability(scheduleId, userId));
  }

  /**
   * Returns the caller's previously submitted availability for the given schedule.
   *
   * @param jwt        the caller's JWT
   * @param scheduleId the schedule's unique identifier
   * @return the caller's availability blocks
   */
  @Operation(summary = "Get My Availability", description = "Returns the caller's previously submitted availability for a schedule.")
  @ApiResponse(responseCode = "200", description = "Availability retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller is not a member of the owning group")
  @ApiResponse(responseCode = "404", description = "Schedule not found")
  @GetMapping("/api/schedules/availability/me")
  public ResponseEntity<AvailabilityResponse> getMyAvailability(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam UUID scheduleId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(availabilityService.getUserAvailability(scheduleId, userId));
  }

  /**
   * Creates a new party for the given schedule. Requires {@code MANAGER} or {@code AUDITOR} role.
   *
   * @param jwt        the caller's JWT
   * @param request    the party request
   * @return the created party
   */
  @Operation(summary = "Create Party", description = "Creates a new party for the schedule. Requires MANAGER or AUDITOR role.")
  @ApiResponse(responseCode = "200", description = "Party created successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller lacks MANAGER or AUDITOR role")
  @ApiResponse(responseCode = "404", description = "Schedule not found")
  @PostMapping("/api/schedules/parties/create")
  public ResponseEntity<PartyResponse> createParty(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody PartyRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(partyService.createParty(request.getScheduleId(), userId, request));
  }

  /**
   * Returns all parties for the given schedule. Requires membership.
   *
   * @param jwt        the caller's JWT
   * @param scheduleId the schedule's unique identifier
   * @return a list of parties ordered by start time
   */
  @Operation(summary = "List Parties", description = "Returns all parties for a schedule. Requires membership.")
  @ApiResponse(responseCode = "200", description = "Parties retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller is not a member of the owning group")
  @ApiResponse(responseCode = "404", description = "Schedule not found")
  @GetMapping("/api/schedules/parties")
  public ResponseEntity<List<PartyResponse>> getScheduleParties(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam UUID scheduleId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(partyService.getScheduleParties(scheduleId, userId));
  }
}
