package com.null0xff.ark.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request payload for updating a party's title and raid type.
 */
@Data
@NoArgsConstructor
@Schema(description = "Request to update a party's title and raid type")
public class PartyUpdateRequest {

  @Schema(description = "Unique identifier of the party to update")
  private UUID partyId;

  @Schema(description = "New display title for the party")
  private String title;

  @Schema(description = "New raid or content type identifier; null leaves existing value unchanged")
  private String raidType;
}
