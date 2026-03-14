package com.null0xff.ark.server.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request payload for updating a user's nickname.
 */
@Schema(description = "Request payload for updating the user's custom nickname")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NicknameRequest {
    
    @Schema(description = "The new nickname to set. Can be empty to revert to Discord username.", example = "CoolAdventurer")
    private String nickname;


}
