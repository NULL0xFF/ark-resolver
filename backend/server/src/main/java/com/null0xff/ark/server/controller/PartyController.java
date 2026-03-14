package com.null0xff.ark.server.controller;

import com.null0xff.ark.server.dto.PartyCompleteRequest;
import com.null0xff.ark.server.dto.PartyJoinRequest;
import com.null0xff.ark.server.dto.PartyUpdateRequest;
import com.null0xff.ark.server.service.PartyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for individual party interactions.
 *
 * <p>Join and leave are open to any group member using one of their registered characters.
 * Delete and completion-status updates require the {@code MANAGER} or {@code AUDITOR} role.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/parties")
@Tag(name = "Parties", description = "Endpoints for joining, leaving, and managing raid parties")
public class PartyController {

  private final PartyService partyService;

  /**
   * Joins a party with a specific registered character, subject to capacity and per-schedule
   * party limits.
   *
   * @param jwt     the caller's JWT
   * @param request the request containing partyId and characterId
   * @return 200 OK on success
   */
  @Operation(summary = "Join Party", description = "Joins a party using a registered character, subject to capacity and per-schedule slot limits.")
  @ApiResponse(responseCode = "200", description = "Joined the party successfully")
  @ApiResponse(responseCode = "400", description = "Party is full, character already joined, or per-schedule limit reached")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller is not a member of the owning group or does not own the character")
  @ApiResponse(responseCode = "404", description = "Party or character not found")
  @PostMapping("/join")
  public ResponseEntity<Void> joinParty(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody PartyJoinRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    partyService.joinParty(request.getPartyId(), request.getCharacterId(), userId);
    return ResponseEntity.ok().build();
  }

  /**
   * Removes a character from a party the caller has previously joined.
   *
   * @param jwt     the caller's JWT
   * @param request the request containing partyId and characterId
   * @return 200 OK on success
   */
  @Operation(summary = "Leave Party", description = "Removes a character from a party.")
  @ApiResponse(responseCode = "200", description = "Left the party successfully")
  @ApiResponse(responseCode = "400", description = "Character is not in the party")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller does not own the character")
  @ApiResponse(responseCode = "404", description = "Party or character not found")
  @PostMapping("/leave")
  public ResponseEntity<Void> leaveParty(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody PartyJoinRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    partyService.leaveParty(request.getPartyId(), request.getCharacterId(), userId);
    return ResponseEntity.ok().build();
  }

  /**
   * Updates a party's title and raid type. Requires {@code MANAGER} or {@code AUDITOR} role.
   *
   * @param jwt     the caller's JWT
   * @param request the update request containing partyId, title, and optionally raidType
   * @return 200 OK on success
   */
  @Operation(summary = "Update Party", description = "Updates a party's title and raid type. Requires MANAGER or AUDITOR role.")
  @ApiResponse(responseCode = "200", description = "Party updated successfully")
  @ApiResponse(responseCode = "400", description = "Title is blank")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller lacks MANAGER or AUDITOR role")
  @ApiResponse(responseCode = "404", description = "Party not found")
  @PostMapping("/update")
  public ResponseEntity<Void> updateParty(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody PartyUpdateRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    partyService.updateParty(request.getPartyId(), userId, request.getTitle(), request.getRaidType());
    return ResponseEntity.ok().build();
  }

  /**
   * Permanently deletes a party. Requires {@code MANAGER} or {@code AUDITOR} role.
   *
   * @param jwt     the caller's JWT
   * @param payload the request containing partyId
   * @return 200 OK on success
   */
  @Operation(summary = "Delete Party", description = "Permanently deletes a raid party. Requires MANAGER or AUDITOR role.")
  @ApiResponse(responseCode = "200", description = "Party deleted successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller lacks MANAGER or AUDITOR role")
  @ApiResponse(responseCode = "404", description = "Party not found")
  @PostMapping("/delete")
  public ResponseEntity<Void> deleteParty(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody Map<String, UUID> payload) {
    UUID userId = UUID.fromString(jwt.getSubject());
    UUID partyId = payload.get("partyId");
    partyService.deleteParty(partyId, userId);
    return ResponseEntity.ok().build();
  }

  /**
   * Marks a party as done or reverts it to planned. Requires {@code MANAGER} or {@code AUDITOR} role.
   *
   * @param jwt     the caller's JWT
   * @param request the party completion request
   * @return 200 OK on success
   */
  @Operation(summary = "Update Party Completion", description = "Marks a party as done or reverts it to planned. Requires MANAGER or AUDITOR role.")
  @ApiResponse(responseCode = "200", description = "Completion status updated successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller lacks MANAGER or AUDITOR role")
  @ApiResponse(responseCode = "404", description = "Party not found")
  @PostMapping("/complete")
  public ResponseEntity<Void> completeParty(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody PartyCompleteRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    partyService.markPartyAsDone(request.getPartyId(), userId, request.getCompleted());
    return ResponseEntity.ok().build();
  }
}
