package com.null0xff.ark.server.controller;

import com.null0xff.ark.server.dto.CharacterRequest;
import com.null0xff.ark.server.dto.CharacterResponse;
import com.null0xff.ark.server.service.CharacterService;
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
 * Controller for managing group-scoped in-game characters.
 *
 * <p>Characters are registered per group by group members. All operations require a valid JWT
 * and group membership. Write operations (create, update, delete) are scoped to the caller's
 * own characters.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/characters")
@Tag(name = "Characters", description = "Endpoints for managing in-game characters within a group")
public class CharacterController {

  private final CharacterService characterService;

  /**
   * Returns all characters registered by the calling user in a specific group.
   *
   * @param jwt     the caller's JWT
   * @param groupId the group's unique identifier
   * @return a list of the caller's characters in that group
   */
  @Operation(summary = "List My Characters", description = "Returns all characters the caller has registered in the specified group.")
  @ApiResponse(responseCode = "200", description = "Characters retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller is not a member of the group")
  @GetMapping("/me")
  public ResponseEntity<List<CharacterResponse>> getMyCharacters(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam UUID groupId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(characterService.getMyCharacters(groupId, userId));
  }

  /**
   * Returns all characters registered by all members in a specific group.
   *
   * @param jwt     the caller's JWT
   * @param groupId the group's unique identifier
   * @return a list of all characters in the group
   */
  @Operation(summary = "List Group Characters", description = "Returns all characters registered by any member in the specified group.")
  @ApiResponse(responseCode = "200", description = "Characters retrieved successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller is not a member of the group")
  @GetMapping
  public ResponseEntity<List<CharacterResponse>> getGroupCharacters(
      @AuthenticationPrincipal Jwt jwt,
      @RequestParam UUID groupId) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(characterService.getGroupCharacters(groupId, userId));
  }

  /**
   * Registers a new character for the calling user in a group.
   *
   * @param jwt     the caller's JWT
   * @param request the character details
   * @return the created character
   */
  @Operation(summary = "Register Character", description = "Registers a new in-game character for the caller within a group.")
  @ApiResponse(responseCode = "200", description = "Character registered successfully")
  @ApiResponse(responseCode = "400", description = "Duplicate character name in this group")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller is not a member of the group")
  @PostMapping("/create")
  public ResponseEntity<CharacterResponse> createCharacter(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody CharacterRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(characterService.createCharacter(userId, request));
  }

  /**
   * Updates an existing character owned by the calling user.
   *
   * @param jwt     the caller's JWT
   * @param request the updated character details; must include {@code characterId}
   * @return the updated character
   */
  @Operation(summary = "Update Character", description = "Updates an existing character owned by the caller.")
  @ApiResponse(responseCode = "200", description = "Character updated successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller does not own the character")
  @ApiResponse(responseCode = "404", description = "Character not found")
  @PostMapping("/update")
  public ResponseEntity<CharacterResponse> updateCharacter(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody CharacterRequest request) {
    UUID userId = UUID.fromString(jwt.getSubject());
    return ResponseEntity.ok(characterService.updateCharacter(userId, request));
  }

  /**
   * Deletes a character owned by the calling user.
   *
   * @param jwt     the caller's JWT
   * @param payload the request containing groupId and characterId
   * @return 200 OK on success
   */
  @Operation(summary = "Delete Character", description = "Deletes a character owned by the caller. Also removes the character from any parties.")
  @ApiResponse(responseCode = "200", description = "Character deleted successfully")
  @ApiResponse(responseCode = "401", description = "Missing or invalid JWT")
  @ApiResponse(responseCode = "403", description = "Caller does not own the character")
  @ApiResponse(responseCode = "404", description = "Character not found")
  @PostMapping("/delete")
  public ResponseEntity<Void> deleteCharacter(
      @AuthenticationPrincipal Jwt jwt,
      @RequestBody Map<String, UUID> payload) {
    UUID userId = UUID.fromString(jwt.getSubject());
    UUID groupId = payload.get("groupId");
    UUID characterId = payload.get("characterId");
    if (groupId == null || characterId == null) {
      return ResponseEntity.badRequest().build();
    }
    characterService.deleteCharacter(groupId, characterId, userId);
    return ResponseEntity.ok().build();
  }
}
