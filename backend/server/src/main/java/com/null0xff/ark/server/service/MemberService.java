package com.null0xff.ark.server.service;

import com.null0xff.ark.server.dto.GroupMemberResponse;
import com.null0xff.ark.server.entity.GroupMember;
import com.null0xff.ark.server.enums.GroupRole;
import com.null0xff.ark.server.exception.ForbiddenException;
import com.null0xff.ark.server.exception.ResourceNotFoundException;
import com.null0xff.ark.server.exception.ValidationException;
import com.null0xff.ark.server.repository.GroupCharacterRepository;
import com.null0xff.ark.server.repository.GroupMemberRepository;
import com.null0xff.ark.server.repository.MemberAvailabilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


/**
 * Service for managing group roster operations.
 */
@Service
@RequiredArgsConstructor
public class MemberService {

    private final GroupMemberRepository groupMemberRepository;
    private final GroupCharacterRepository characterRepository;
    private final MemberAvailabilityRepository memberAvailabilityRepository;
    private final AuthorizationHelper authorizationHelper;

    public List<GroupMemberResponse> getGroupMembers(UUID groupId, UUID userId) {
        authorizationHelper.requireMembership(groupId, userId);

        return groupMemberRepository.findByGroupId(groupId).stream()
                .map(member -> {
                    String name = member.getUser().getNickname() != null && !member.getUser().getNickname().isEmpty()
                            ? member.getUser().getNickname()
                            : member.getUser().getUsername();
                    return new GroupMemberResponse(
                            member.getUser().getId(),
                            name,
                            member.getRole().name(),
                            member.getUser().getDiscordId(),
                            member.getUser().getAvatar()
                    );
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void removeMember(UUID groupId, UUID userId, UUID targetUserId) {
        GroupMember actor = authorizationHelper.requireMembership(groupId, userId);

        GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in group", targetUserId));

        if (!userId.equals(targetUserId) && actor.getRole() != GroupRole.MANAGER) {
            throw new ForbiddenException("Only managers can remove other members", groupId);
        }

        if (target.getRole() == GroupRole.MANAGER) {
            long managerCount = groupMemberRepository.findByGroupId(groupId).stream()
                    .filter(m -> m.getRole() == GroupRole.MANAGER).count();
            if (managerCount <= 1 && userId.equals(targetUserId)) {
                throw new ValidationException("Cannot leave group as the only manager. Transfer ownership or delete the group instead.", groupId);
            }
        }

        characterRepository.deleteByUserIdAndGroupId(targetUserId, groupId);
        memberAvailabilityRepository.deleteByUserIdAndGroupId(targetUserId, groupId);
        groupMemberRepository.delete(target);
    }

    @Transactional
    public void updateMemberRole(UUID groupId, UUID managerId, UUID targetUserId, GroupRole newRole) {
        authorizationHelper.requireRole(groupId, managerId, GroupRole.MANAGER);

        if (managerId.equals(targetUserId)) {
            throw new ValidationException("Cannot change your own role. Use the manager transfer feature instead.", groupId);
        }

        if (newRole == GroupRole.MANAGER) {
            throw new ValidationException("Use the transfer endpoint to promote someone to MANAGER", groupId);
        }

        GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found in group", targetUserId));

        target.setRole(newRole);
        groupMemberRepository.save(target);
    }

    @Transactional
    public void transferManager(UUID groupId, UUID currentManagerId, UUID newManagerId) {
        if (currentManagerId.equals(newManagerId)) {
            throw new ValidationException("Cannot transfer management to yourself", groupId);
        }

        GroupMember currentManager = authorizationHelper.requireRole(groupId, currentManagerId, GroupRole.MANAGER);

        GroupMember newManager = groupMemberRepository.findByGroupIdAndUserId(groupId, newManagerId)
                .orElseThrow(() -> new ResourceNotFoundException("Target user is not a member of this group", newManagerId));

        newManager.setRole(GroupRole.MANAGER);
        currentManager.setRole(GroupRole.MEMBER);
        groupMemberRepository.save(newManager);
        groupMemberRepository.save(currentManager);
    }
}
