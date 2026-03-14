package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request payload for generating a new invite code.
 */
@Schema(description = "Request payload for generating a new invite code")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InviteCodeRequest {

    @Schema(description = "Maximum number of times this code can be used", example = "5")
    private java.util.UUID groupId;

    @io.swagger.v3.oas.annotations.media.Schema(description = "Maximum number of times the code can be used", example = "5")
    private Integer maxUsage;

    @Schema(description = "Number of days until the code expires", example = "7")
    private Integer expirationDays;




}
