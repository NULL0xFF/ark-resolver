package com.null0xff.ark.server.service;

import com.null0xff.ark.server.dto.GroupResponse;
import com.null0xff.ark.server.entity.Group;
import com.null0xff.ark.server.entity.GroupMember;
import com.null0xff.ark.server.enums.GroupRole;
import com.null0xff.ark.server.exception.ResourceNotFoundException;
import com.null0xff.ark.server.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for core group CRUD operations.
 */
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final AuthorizationHelper authorizationHelper;

    public GroupResponse getGroupDetails(UUID groupId, UUID userId) {
        authorizationHelper.requireMembership(groupId, userId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Group not found", groupId));
        return new GroupResponse(group.getId(), group.getName(), group.getDescription(), group.getMaxPartiesPerCharacter());
    }

    @Transactional
    public void updateGroup(UUID groupId, UUID managerId, String name, String description, Integer maxPartiesPerCharacter) {
        GroupMember manager = authorizationHelper.requireRole(groupId, managerId, GroupRole.MANAGER);

        Group group = manager.getGroup();
        group.setName(name);
        group.setDescription(description);
        if (maxPartiesPerCharacter != null) {
            if (maxPartiesPerCharacter < 1 || maxPartiesPerCharacter > 10) {
                throw new IllegalArgumentException("maxPartiesPerCharacter must be between 1 and 10");
            }
            group.setMaxPartiesPerCharacter(maxPartiesPerCharacter);
        }
        groupRepository.save(group);
    }

    @Transactional
    public void deleteGroup(UUID groupId, UUID managerId) {
        authorizationHelper.requireRole(groupId, managerId, GroupRole.MANAGER);
        groupRepository.deleteById(groupId);
    }
}
