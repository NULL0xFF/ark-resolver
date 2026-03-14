package com.null0xff.ark.server.controller;

import com.null0xff.ark.server.dto.InviteCodeRequest;
import com.null0xff.ark.server.dto.InviteCodeResponse;
import com.null0xff.ark.server.dto.JoinGroupRequest;
import com.null0xff.ark.server.service.InviteService;
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
 * Controller for group invite operations.
 *
 * <p>Generating and viewing invite codes requires {@code MANAGER} or {@code AUDITOR} role.
 * Joining via code is open to any authenticated user.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Invites", description = "Endpoints for generating and using group invite codes")
public class InviteController {

  private final InviteService inviteService;

  /**
   * Generates a new 8-character alphanumeric invite code. Requires {@code MANAGER} or {@code AUDITOR} role.
   *
   * @param jwt     the caller's JWT
   * @param request the invite code request
   * @return the newly created invite code details
   */
  @Operation(summary = "Generate Invite Code", description = "Creates a new invite code. Requires MANAGER or AUDITOR role.")
  @ApiResponse(responseCode = "200", description = "Invite code generated successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller lacks MANAGER or AUDITOR role")
  @PostMapping("/api/invites/create")
  public ResponseEntity<InviteCodeResponse> generateInviteCode(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody InviteCodeRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(inviteService.generateInviteCode(
        request.getGroupId(), userId, request.getMaxUsage(), request.getExpirationDays()));
  }

  /**
   * Retrieves all active invite codes for the specified group. Requires {@code MANAGER} or {@code AUDITOR} role.
   *
   * @param jwt     the caller's JWT
   * @param groupId the group's unique identifier
   * @return a list of active invite codes
   */
  @Operation(summary = "List Active Invites", description = "Returns all active invite codes for the group. Requires MANAGER or AUDITOR role.")
  @ApiResponse(responseCode = "200", description = "Invite codes retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller lacks MANAGER or AUDITOR role")
  @GetMapping("/api/invites/list")
  public ResponseEntity<List<InviteCodeResponse>> getActiveInviteCodes(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam UUID groupId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(inviteService.getActiveInviteCodes(groupId, userId));
  }

  /**
   * Revokes a specific invite code. Requires {@code MANAGER} or {@code AUDITOR} role.
   *
   * @param jwt     the caller's JWT
   * @param payload the request containing groupId and code
   * @return 200 OK on success
   */
  @Operation(summary = "Revoke Invite Code", description = "Permanently revokes an invite code. Requires MANAGER or AUDITOR role.")
  @ApiResponse(responseCode = "200", description = "Invite code revoked successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller lacks MANAGER or AUDITOR role, or code belongs to a different group")
  @ApiResponse(responseCode = "404", description = "Invite code not found")
  @PostMapping("/api/invites/revoke")
  public ResponseEntity<Void> revokeInviteCode(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody Map<String, Object> payload) {
    UUID userId = UUID.fromString(jwt.getSubject());
    UUID groupId = UUID.fromString(payload.get("groupId").toString());
    String code = payload.get("code").toString();
    inviteService.revokeInviteCode(groupId, userId, code);
    return ResponseEntity.ok().build();
  }

  /**
   * Joins a group using a valid invite code. Open to any authenticated user.
   *
   * @param jwt     the caller's JWT
   * @param request the invite code string
   * @return 200 OK on success
   */
  @Operation(summary = "Join Group", description = "Joins a group using a valid invite code.")
  @ApiResponse(responseCode = "200", description = "Joined the group successfully")
  @ApiResponse(responseCode = "400", description = "Code is invalid, expired, exhausted, or user is already a member")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @PostMapping("/api/groups/join")
  public ResponseEntity<Void> joinGroup(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody JoinGroupRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    inviteService.joinGroup(userId, request.getCode());
    return ResponseEntity.ok().build();
  }
}