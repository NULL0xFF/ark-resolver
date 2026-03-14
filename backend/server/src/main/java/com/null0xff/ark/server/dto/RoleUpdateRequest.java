package com.null0xff.ark.server.dto;

import com.null0xff.ark.server.enums.GroupRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for updating a group member's role.
 *
 * <p>Only {@code MEMBER} and {@code AUDITOR} are accepted; promoting to {@code MANAGER}
 * requires the dedicated transfer endpoint.
 */
@Data
@NoArgsConstructor
@Schema(description = "Request to update a member's role within a group")
public class RoleUpdateRequest {

  @Schema(description = "The new role to assign; MANAGER is not permitted here", example = "AUDITOR")
  private java.util.UUID groupId;
  private java.util.UUID targetUserId;
  @Schema(description = "The new role to assign; MANAGER is not permitted here", example = "AUDITOR")
  private GroupRole role;
}
