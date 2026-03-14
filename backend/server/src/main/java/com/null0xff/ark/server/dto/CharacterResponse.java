package com.null0xff.ark.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Response payload representing a group-scoped in-game character.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response payload representing a registered character within a group")
public class CharacterResponse {

  @Schema(description = "Unique identifier of the character")
  private UUID id;

  @Schema(description = "Unique identifier of the owning user")
  private UUID userId;

  @Schema(description = "In-game character name", example = "Aether")
  private String name;

  @Schema(description = "In-game character class", example = "Bard")
  private String characterClass;

  @Schema(description = "Current item level", example = "1620.0")
  private Double itemLevel;
}
