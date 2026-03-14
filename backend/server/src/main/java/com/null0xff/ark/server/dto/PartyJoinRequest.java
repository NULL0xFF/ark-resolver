package com.null0xff.ark.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for joining a raid party with a specific character.
 */
@Data
@NoArgsConstructor
@Schema(description = "Request payload for joining a party with a registered character")
public class PartyJoinRequest {

  @Schema(description = "Unique identifier of the party to join", requiredMode = Schema.RequiredMode.REQUIRED)
  private UUID partyId;

  @Schema(description = "Unique identifier of the character to use for this slot", requiredMode = Schema.RequiredMode.REQUIRED)
  private UUID characterId;
}
