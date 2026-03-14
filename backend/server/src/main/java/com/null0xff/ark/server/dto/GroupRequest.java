package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Request payload for creating or updating a group.
 */
@Schema(description = "Request payload for group creation or update")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupRequest {

  @Schema(description = "The unique identifier of the group; required for update operations")
  private UUID groupId;

  @Schema(description = "The name of the group", example = "Friday Night Static",
      requiredMode = Schema.RequiredMode.REQUIRED)
  private String name;

  @Schema(description = "Optional description of the group", example = "Weekly raids and horizontal content.")
  private String description;

  @Schema(description = "Maximum number of parties a character may join within a single schedule; defaults to 3",
      example = "3")
  private Integer maxPartiesPerCharacter;
}
