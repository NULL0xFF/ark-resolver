package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response payload for group information.
 */
@Schema(description = "Response payload for group details")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupResponse {

  @Schema(description = "The unique identifier of the group")
  private UUID id;

  @Schema(description = "The name of the group")
  private String name;

  @Schema(description = "The description of the group")
  private String description;

  @Schema(description = "Maximum number of parties a character may join within a single schedule")
  private Integer maxPartiesPerCharacter;
}
