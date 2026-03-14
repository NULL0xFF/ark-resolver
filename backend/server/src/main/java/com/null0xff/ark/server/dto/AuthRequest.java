package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object for authentication requests.
 * Contains the OAuth2 authorization code received from Discord.
 */
@Schema(description = "Request payload containing the Discord OAuth2 authorization code")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {
    
    @Schema(description = "The authorization code returned by Discord", example = "a6ML2UdpsqBozEXBKDjJaARBfqpqm1", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;


}
