package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Response payload representing a user's availability.
 */
@Schema(description = "User's availability for a schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityResponse {

    @Schema(description = "The User ID")
    private UUID userId;

    @Schema(description = "List of available time blocks")
    private List<AvailabilityBlock> blocks;





}
