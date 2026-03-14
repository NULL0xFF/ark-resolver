package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Request payload to create a new party.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PartyRequest {

    @Schema(description = "Title of the party")
    private java.util.UUID scheduleId;
    @io.swagger.v3.oas.annotations.media.Schema(description = "Title of the party")
    private String title;

    @Schema(description = "Type of raid")
    private String raidType;

    @Schema(description = "Maximum number of members")
    private Integer maxMembers;

    @Schema(description = "Exact start time of the party")
    private Instant startTime;








}
