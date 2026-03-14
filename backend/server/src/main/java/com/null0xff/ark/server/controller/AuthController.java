package com.null0xff.ark.server.controller;

import com.null0xff.ark.server.dto.AuthRequest;
import com.null0xff.ark.server.dto.AuthResponse;
import com.null0xff.ark.server.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.UUID;

/**
 * Controller for handling authentication-related requests.
 * Provides endpoints for Discord OAuth2 login flow and token refreshing.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for managing user authentication and JWT tokens")
public class AuthController {

    private final AuthService authService;

    @Value("${app.oauth2.discord.client-id}")
    private String clientId;

    @Value("${app.oauth2.discord.redirect-uri}")
    private String redirectUri;

    /**
     * Refreshes the current user's JWT token.
     * Requires a valid JWT token in the Authorization header.
     *
     * @param jwt The current JWT token of the authenticated user
     * @return A new AuthResponse containing the refreshed JWT token
     */
    @Operation(summary = "Refresh JWT Token", description = "Generates a new JWT token for an already authenticated user, extending their session.")
    @ApiResponse(responseCode = "200", description = "Token refreshed successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized or invalid token")
    @GetMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@AuthenticationPrincipal Jwt jwt) {
        try {
            UUID userId = UUID.fromString(jwt.getSubject());
            String newToken = authService.refreshToken(userId);
            return ResponseEntity.ok(new AuthResponse(newToken));
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }

    /**
     * Retrieves the Discord OAuth2 authorization URL.
     * The frontend uses this URL to redirect the user to Discord for authentication.
     *
     * @return A map containing the generated authorization URL
     */
    @Operation(summary = "Get Discord Auth URL", description = "Returns the dynamically generated URL for Discord OAuth2 authorization.")
    @ApiResponse(responseCode = "200", description = "URL generated successfully")
    @GetMapping("/discord/url")
    public ResponseEntity<Map<String, String>> getDiscordAuthUrl() {
        String authUrl = "https://discord.com/api/oauth2/authorize" +
                "?client_id=" + clientId +
                "&redirect_uri=" + redirectUri +
                "&response_type=code" +
                "&scope=identify";
        return ResponseEntity.ok(Map.of("url", authUrl));
    }

    /**
     * Authenticates a user using a Discord authorization code.
     * Exchanges the code for a Discord access token, retrieves the user profile,
     * and generates a custom JWT token for the application.
     *
     * @param request The AuthRequest containing the authorization code
     * @return An AuthResponse containing the generated JWT token
     */
    @Operation(summary = "Login with Discord", description = "Exchanges a Discord OAuth2 authorization code for a custom JWT access token.")
    @ApiResponse(responseCode = "200", description = "Successfully authenticated")
    @ApiResponse(responseCode = "400", description = "Invalid authorization code or failed authentication")
    @PostMapping("/discord")
    public ResponseEntity<AuthResponse> loginWithDiscord(@RequestBody AuthRequest request) {
        try {
            String token = authService.loginWithDiscord(request.getCode());
            return ResponseEntity.ok(new AuthResponse(token));
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
