package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Request payload to submit user availability for a schedule.
 */
@Schema(description = "Request payload to submit user availability")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvailabilityRequest {

    @Schema(description = "List of available time blocks")
    private java.util.UUID scheduleId;
    @io.swagger.v3.oas.annotations.media.Schema(description = "List of available time blocks")
    private List<AvailabilityBlock> blocks;


}
