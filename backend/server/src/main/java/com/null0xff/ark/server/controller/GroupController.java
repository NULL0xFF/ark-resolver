package com.null0xff.ark.server.controller;

import com.null0xff.ark.server.dto.GroupRequest;
import com.null0xff.ark.server.dto.GroupResponse;
import com.null0xff.ark.server.entity.Group;
import com.null0xff.ark.server.service.GroupService;
import com.null0xff.ark.server.service.UserService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for core group CRUD operations.
 *
 * <p>All endpoints require a valid JWT. Mutation endpoints
 * additionally require the caller to hold the {@code MANAGER} role in the target group.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
@Tag(name = "Groups", description = "Endpoints for managing group collectives")
public class GroupController {

  private final UserService userService;
  private final GroupService groupService;

  /**
   * Returns all groups the authenticated user belongs to.
   *
   * @param jwt the caller's JWT
   * @return a list of group summaries
   */
  @Operation(summary = "List My Groups", description = "Returns all groups the current user is a member of.")
  @ApiResponse(responseCode = "200", description = "Groups retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @GetMapping
  public ResponseEntity<List<GroupResponse>> listMyGroups(@AuthenticationPrincipal Jwt jwt) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(userService.getUserGroups(userId));
  }

  /**
   * Creates a new group and assigns the caller as its initial {@code MANAGER}.
   *
   * @param jwt     the caller's JWT
   * @param request the group name and description
   * @return the newly created group
   */
  @Operation(summary = "Create Group", description = "Creates a new group and makes the caller the initial MANAGER.")
  @ApiResponse(responseCode = "200", description = "Group created successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @PostMapping
  public ResponseEntity<GroupResponse> createGroup(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody GroupRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    Group group = userService.createGroup(userId, request.getName(), request.getDescription());
    return ResponseEntity.ok(new GroupResponse(group.getId(), group.getName(), group.getDescription(), group.getMaxPartiesPerCharacter()));
  }

  /**
   * Retrieves details of a specific group. Requires membership.
   *
   * @param jwt     the caller's JWT
   * @param groupId the group's unique identifier
   * @return the group details
   */
  @Operation(summary = "Get Group Details", description = "Retrieves details of a specific group. Requires membership.")
  @ApiResponse(responseCode = "200", description = "Group retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller is not a member of the group")
  @ApiResponse(responseCode = "404", description = "Group not found")
  @GetMapping("/detail")
  public ResponseEntity<GroupResponse> getGroupDetails(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam UUID groupId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(groupService.getGroupDetails(groupId, userId));
  }

  /**
   * Updates the group's name and description. Requires {@code MANAGER} role.
   *
   * @param jwt     the caller's JWT
   * @param request the updated name, description, and target groupId
   * @return 200 OK on success
   */
  @Operation(summary = "Update Group Settings", description = "Updates group name and description. Requires MANAGER role.")
  @ApiResponse(responseCode = "200", description = "Group updated successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller does not hold the MANAGER role")
  @ApiResponse(responseCode = "404", description = "Group not found")
  @PostMapping("/update")
  public ResponseEntity<Void> updateGroup(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody GroupRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    groupService.updateGroup(request.getGroupId(), userId, request.getName(), request.getDescription(), request.getMaxPartiesPerCharacter());
    return ResponseEntity.ok().build();
  }

  /**
   * Permanently deletes the group and all associated data. Requires {@code MANAGER} role.
   *
   * @param jwt     the caller's JWT
   * @param payload the request containing groupId
   * @return 200 OK on success
   */
  @Operation(summary = "Delete Group", description = "Permanently deletes the group and all associated data. Requires MANAGER role.")
  @ApiResponse(responseCode = "200", description = "Group deleted successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller does not hold the MANAGER role")
  @ApiResponse(responseCode = "404", description = "Group not found")
  @PostMapping("/delete")
  public ResponseEntity<Void> deleteGroup(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody Map<String, UUID> payload) {
    UUID userId = UUID.fromString(jwt.getSubject());
    UUID groupId = payload.get("groupId");
    groupService.deleteGroup(groupId, userId);
    return ResponseEntity.ok().build();
  }
}
