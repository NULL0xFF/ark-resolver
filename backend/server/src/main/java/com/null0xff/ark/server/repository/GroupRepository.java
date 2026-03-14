package com.null0xff.ark.server.repository;

import com.null0xff.ark.server.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repository for {@link Group} entities.
 *
 * <p>Relies entirely on standard {@link JpaRepository} operations; no custom queries
 * are required because group lookups are always by primary key.
 */
@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {}
