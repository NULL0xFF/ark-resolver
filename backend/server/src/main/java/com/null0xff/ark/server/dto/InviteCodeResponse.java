package com.null0xff.ark.server.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Response payload representing a group invite code.
 *
 * <p>The expiry timestamp is normalised to a UTC calendar date on construction so the
 * client does not need to perform timezone arithmetic.
 */
@Data
@NoArgsConstructor
@Schema(description = "Response payload representing an invite code")
public class InviteCodeResponse {

  @Schema(description = "The unique invite code string", example = "ARK-AB12-CD34")
  private String code;

  @Schema(description = "Maximum number of times this code can be used; null means unlimited")
  private Integer max;

  @Schema(description = "Number of times this code has already been used")
  private Integer used;

  @Schema(description = "Expiry date in UTC (YYYY-MM-DD)", type = "string", format = "date", example = "2025-02-01")
  private String expires;

  /**
   * Constructs an {@code InviteCodeResponse}, converting the expiry instant to a UTC date string.
   *
   * @param code      the unique invite code string
   * @param max       maximum usage count; {@code null} means unlimited
   * @param used      number of times the code has already been used
   * @param expiresAt the expiry instant; may be {@code null}
   */
  public InviteCodeResponse(String code, Integer max, Integer used, Instant expiresAt) {
    this.code = code;
    this.max = max;
    this.used = used;
    this.expires = expiresAt != null
        ? expiresAt.atZone(ZoneOffset.UTC).toLocalDate().toString()
        : null;
  }
}
