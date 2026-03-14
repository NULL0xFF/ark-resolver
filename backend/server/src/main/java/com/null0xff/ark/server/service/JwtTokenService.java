package com.null0xff.ark.server.service;

import com.null0xff.ark.server.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Service responsible for generating and parsing custom JSON Web Tokens (JWT).
 * Used for stateless authentication across the application.
 */
@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration-ms}")
    private long expirationMs;

    @PostConstruct
    public void init() {
        String workingDir = System.getProperty("user.dir");
        logger.info("Current Working Directory: {}", workingDir);
        logger.info("JWT Token Service initialized with expiration: {}ms", expirationMs);
        if (jwtSecret.equals("your-256-bit-secret-key-goes-here-change-me")) {
            logger.warn("JWT Secret is using the DEFAULT value. Please check your .env file!");
        } else {
            logger.info("JWT Secret is using a CUSTOM value from environment (length: {})", jwtSecret.length());
        }
    }

    /**
     * Generates a new JWT signed with HMAC-SHA256.
     * Contains standard claims (issuer, iat, exp, sub) and custom claims for the user's Discord profile.
     *
     * @param user The user for whom the token is being generated
     * @return The serialized JWT string
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("ark-resolver")
                .issuedAt(now)
                .expiresAt(now.plusMillis(expirationMs))
                .subject(user.getId().toString())
                .claim("discordId", user.getDiscordId())
                .claim("username", user.getUsername())
                .build();

        SecretKeySpec secretKey = new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    /**
     * Parses a JWT string and extracts the "Issued At" (iat) timestamp.
     * Useful for token whitelisting and session validation.
     *
     * @param token The raw JWT string
     * @return The Instant representing when the token was issued
     */
    public java.time.Instant extractIat(String token) {
        try {
            return com.nimbusds.jwt.SignedJWT.parse(token).getJWTClaimsSet().getIssueTime().toInstant();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse token for IAT", e);
        }
    }
}
