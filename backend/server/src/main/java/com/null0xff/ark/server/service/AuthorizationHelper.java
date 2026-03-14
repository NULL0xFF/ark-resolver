package com.null0xff.ark.server.service;

import com.null0xff.ark.server.entity.GroupMember;
import com.null0xff.ark.server.enums.GroupRole;
import com.null0xff.ark.server.exception.ForbiddenException;
import com.null0xff.ark.server.repository.GroupMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

/**
 * Centralised helper for group membership and role checks.
 * Eliminates repeated repository lookups + role assertions across services.
 */
@Service
@RequiredArgsConstructor
public class AuthorizationHelper {

    private final GroupMemberRepository groupMemberRepository;

    /**
     * Returns the membership record if the user is a member of the group.
     *
     * @throws ForbiddenException if the user is not a member
     */
    public GroupMember requireMembership(UUID groupId, UUID userId) {
        return groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new ForbiddenException("Access denied", groupId));
    }

    /**
     * Returns the membership record if the user holds one of the allowed roles.
     *
     * @throws ForbiddenException if the user is not a member or lacks the required role
     */
    public GroupMember requireRole(UUID groupId, UUID userId, GroupRole... allowed) {
        GroupMember member = requireMembership(groupId, userId);
        if (Arrays.stream(allowed).noneMatch(r -> r == member.getRole())) {
            throw new ForbiddenException("Insufficient permissions", groupId);
        }
        return member;
    }
}
