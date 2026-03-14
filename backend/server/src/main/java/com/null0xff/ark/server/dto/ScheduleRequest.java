package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Request payload for creating a new schedule.
 */
@Schema(description = "Request payload for creating a new schedule")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRequest {

    @Schema(description = "The title of the schedule", example = "March Week 1 Reset", requiredMode = Schema.RequiredMode.REQUIRED)
    private java.util.UUID groupId;
    private java.util.UUID scheduleId;

    @io.swagger.v3.oas.annotations.media.Schema(description = "The title of the schedule", example = "March Week 1 Reset", requiredMode = io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "The start date and time of the schedule in ISO format", example = "2026-03-04T10:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant startTime;

    @Schema(description = "The end date and time of the schedule in ISO format", example = "2026-03-11T05:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED)
    private Instant endTime;






}
