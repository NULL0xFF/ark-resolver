package com.null0xff.ark.server.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a registered user within the Ark Resolver system.
 * Users are authenticated primarily via Discord OAuth2.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "discord_id", unique = true, nullable = false)
    private String discordId;

    @Column(nullable = false)
    private String username;

    private String nickname;

    private String avatar;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "last_login")
    private Instant lastLogin;

    @Column(name = "last_issued_at")
    private java.time.Instant lastIssuedAt;
















    public java.time.Instant getLastIssuedAt() {
        return lastIssuedAt;
    }

}
