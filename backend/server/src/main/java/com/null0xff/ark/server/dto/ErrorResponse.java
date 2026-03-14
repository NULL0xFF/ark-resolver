package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Standardized error response payload for the API.
 */
@Schema(description = "Standardized error response payload")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    
    @Schema(description = "The error message", example = "User not found")
    private String message;

    @Schema(description = "The HTTP status code", example = "400")
    private int status;

    @Schema(description = "The UUID of the related resource (optional)", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID resourceId;

    public ErrorResponse(String message, int status) {
        this.message = message;
        this.status = status;
    }
}
