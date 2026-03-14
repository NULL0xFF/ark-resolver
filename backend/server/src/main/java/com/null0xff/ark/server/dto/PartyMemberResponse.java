package com.null0xff.ark.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response payload representing a character slot within a raid party.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload for a character occupying a slot in a raid party")
public class PartyMemberResponse {

  @Schema(description = "Unique identifier of the character")
  private UUID characterId;

  @Schema(description = "In-game character name", example = "Aether")
  private String characterName;

  @Schema(description = "In-game character class", example = "Bard")
  private String characterClass;

  @Schema(description = "Current item level of the character", example = "1620.0")
  private Double itemLevel;

  @Schema(description = "Unique identifier of the user who owns this character")
  private UUID userId;
}
