package com.null0xff.ark.server.service;

import com.null0xff.ark.server.dto.InviteCodeResponse;
import com.null0xff.ark.server.entity.GroupMember;
import com.null0xff.ark.server.entity.InviteCode;
import com.null0xff.ark.server.entity.User;
import com.null0xff.ark.server.enums.GroupRole;
import com.null0xff.ark.server.exception.ResourceNotFoundException;
import com.null0xff.ark.server.exception.ValidationException;
import com.null0xff.ark.server.repository.GroupMemberRepository;
import com.null0xff.ark.server.repository.InviteCodeRepository;
import com.null0xff.ark.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing group invite codes and the join-by-code flow.
 */
@Service
@RequiredArgsConstructor
public class InviteService {

    private final InviteCodeRepository inviteCodeRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final AuthorizationHelper authorizationHelper;

    @Transactional
    public InviteCodeResponse generateInviteCode(UUID groupId, UUID userId, Integer maxUsage, Integer expirationDays) {
        GroupMember member = authorizationHelper.requireRole(groupId, userId, GroupRole.MANAGER, GroupRole.AUDITOR);

        String codeStr = "ARK-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase()
                + "-" + UUID.randomUUID().toString().substring(4, 8).toUpperCase();

        InviteCode code = new InviteCode();
        code.setCode(codeStr);
        code.setGroup(member.getGroup());
        code.setCreatedBy(member.getUser());
        code.setMaxUsage(maxUsage);
        code.setExpiresAt(Instant.now().plus(expirationDays, ChronoUnit.DAYS));
        inviteCodeRepository.save(code);

        return new InviteCodeResponse(code.getCode(), code.getMaxUsage(), code.getCurrentUsage(), code.getExpiresAt());
    }

    public List<InviteCodeResponse> getActiveInviteCodes(UUID groupId, UUID userId) {
        authorizationHelper.requireRole(groupId, userId, GroupRole.MANAGER, GroupRole.AUDITOR);

        return inviteCodeRepository.findByGroupId(groupId).stream()
                .filter(code -> code.getExpiresAt().isAfter(Instant.now())
                        && (code.getMaxUsage() == null || code.getCurrentUsage() < code.getMaxUsage()))
                .map(code -> new InviteCodeResponse(code.getCode(), code.getMaxUsage(), code.getCurrentUsage(), code.getExpiresAt()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeInviteCode(UUID groupId, UUID userId, String codeStr) {
        authorizationHelper.requireRole(groupId, userId, GroupRole.MANAGER, GroupRole.AUDITOR);

        InviteCode code = inviteCodeRepository.findByCode(codeStr)
                .orElseThrow(() -> new ResourceNotFoundException("Code not found"));

        if (!code.getGroup().getId().equals(groupId)) {
            throw new ValidationException("Code does not belong to this group", groupId);
        }

        inviteCodeRepository.delete(code);
    }

    @Transactional
    public void joinGroup(UUID userId, String codeStr) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", userId));

        InviteCode code = inviteCodeRepository.findByCode(codeStr)
                .orElseThrow(() -> new ValidationException("Invalid or expired invite code"));

        if (code.getExpiresAt().isBefore(Instant.now())) {
            throw new ValidationException("Invite code has expired");
        }

        if (code.getMaxUsage() != null && code.getCurrentUsage() >= code.getMaxUsage()) {
            throw new ValidationException("Invite code usage limit reached");
        }

        if (groupMemberRepository.findByGroupIdAndUserId(code.getGroup().getId(), userId).isPresent()) {
            throw new ValidationException("You are already a member of this group", code.getGroup().getId());
        }

        GroupMember newMember = new GroupMember();
        newMember.setGroup(code.getGroup());
        newMember.setUser(user);
        newMember.setRole(GroupRole.MEMBER);
        groupMemberRepository.save(newMember);

        code.setCurrentUsage(code.getCurrentUsage() + 1);
        inviteCodeRepository.save(code);
    }
}
