package com.null0xff.ark.server.repository;

import com.null0xff.ark.server.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for {@link User} entities.
 *
 * <p>Provides lookup by Discord snowflake ID, which is used during the OAuth2 login
 * flow to find or create local user records.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

  /**
   * Returns the user associated with the given Discord snowflake ID.
   *
   * @param discordId the user's Discord snowflake ID
   * @return an {@link Optional} containing the user, or empty if not yet registered
   */
  Optional<User> findByDiscordId(String discordId);
}
