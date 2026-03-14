package com.null0xff.ark.server.controller;

import com.null0xff.ark.server.dto.NicknameRequest;
import com.null0xff.ark.server.entity.User;
import com.null0xff.ark.server.exception.ResourceNotFoundException;
import com.null0xff.ark.server.repository.UserRepository;
import com.null0xff.ark.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * Controller for handling user-specific operations.
 * Requires an authenticated user session (JWT).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User Profile", description = "Endpoints for retrieving and managing user profile information")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    /**
     * Retrieves the profile information of the currently authenticated user.
     *
     * @param jwt The decoded JWT token representing the current user
     * @return A map containing the user's profile details
     */
    @Operation(summary = "Get Current User", description = "Returns the profile details of the user associated with the provided JWT access token.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved user profile")
    @ApiResponse(responseCode = "401", description = "Unauthorized - Missing or invalid token")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("id", user.getId());
        body.put("discordId", user.getDiscordId());
        body.put("username", user.getUsername());
        body.put("nickname", user.getNickname());
        body.put("avatar", user.getAvatar());
        return ResponseEntity.ok(body);
    }

    /**
     * Updates the custom nickname for the currently authenticated user.
     *
     * @param jwt     The current user's JWT
     * @param request The request body containing the new nickname
     * @return 200 OK if successful
     */
    @Operation(summary = "Update Nickname", description = "Sets a custom display name for the user.")
    @ApiResponse(responseCode = "200", description = "Nickname updated successfully")
    @PostMapping("/me/nickname")
    public ResponseEntity<Void> updateNickname(@AuthenticationPrincipal Jwt jwt, @RequestBody NicknameRequest request) {
        UUID userId = UUID.fromString(jwt.getSubject());
        userService.updateNickname(userId, request.getNickname());
        return ResponseEntity.ok().build();
    }

    /**
     * Deletes the currently authenticated user's account.
     *
     * @param jwt The current user's JWT
     * @return 200 OK if successful
     */
    @Operation(summary = "Delete Account", description = "Permanently deletes the user's account and all associated data.")
    @ApiResponse(responseCode = "200", description = "Account deleted successfully")
    @PostMapping("/me/delete")
    public ResponseEntity<Void> deleteAccount(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        userService.deleteAccount(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Placeholder for syncing the user's profile with Discord.
     */
    @Operation(summary = "Sync with Discord", description = "Placeholder endpoint for manually syncing Discord profile data (username, avatar).")
    @ApiResponse(responseCode = "200", description = "Sync completed successfully (mock)")
    @PostMapping("/me/sync")
    public ResponseEntity<Void> syncProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().build();
    }
}
