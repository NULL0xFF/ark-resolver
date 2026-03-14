package com.null0xff.ark.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Request to update party completion status")
public class PartyCompleteRequest {

    @Schema(description = "Whether the party is completed or not", example = "true")
    private java.util.UUID partyId;
    @Schema(description = "Whether the party is completed or not", example = "true")
    private Boolean completed;
}
