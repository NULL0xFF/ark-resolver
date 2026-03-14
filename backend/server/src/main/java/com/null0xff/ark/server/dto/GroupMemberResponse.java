package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * Response payload representing a member of a group.
 */
@Schema(description = "Response payload representing a group member")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupMemberResponse {

    @Schema(description = "User ID")
    private UUID id;

    @Schema(description = "User's display name (nickname or fallback to discord username)")
    private String username;

    @Schema(description = "User's role in the group (MANAGER, MEMBER)")
    private String role;
    
    @Schema(description = "Discord user ID for avatar fallback")
    private String discordId;
    
    @Schema(description = "Discord avatar hash")
    private String avatar;











}
