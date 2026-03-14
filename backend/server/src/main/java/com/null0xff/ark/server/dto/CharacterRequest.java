package com.null0xff.ark.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for creating or updating a group-scoped character.
 */
@Data
@NoArgsConstructor
@Schema(description = "Request payload for registering or updating a character in a group")
public class CharacterRequest {

  @Schema(description = "The group the character belongs to", requiredMode = Schema.RequiredMode.REQUIRED)
  private UUID groupId;

  @Schema(description = "The character's unique identifier; required for update operations")
  private UUID characterId;

  @Schema(description = "In-game character name", example = "Aether", requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(description = "In-game character class", example = "Bard")
  private String characterClass;

  @Schema(description = "Current item level of the character", example = "1620.0")
  private Double itemLevel;
}
