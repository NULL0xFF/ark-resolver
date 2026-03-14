package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request payload for joining a group via invite code.
 */
@Schema(description = "Request payload for joining a group using an invite code")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinGroupRequest {

    @Schema(description = "The invite code to join the group", example = "ARK-ABCD-1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;


}
