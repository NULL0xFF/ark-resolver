package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Data Transfer Object for authentication responses.
 * Contains the JWT token used for authenticating subsequent API requests.
 */
@Schema(description = "Response payload containing the generated JWT token")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    @Schema(description = "The JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;



}
