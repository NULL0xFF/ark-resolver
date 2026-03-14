package com.null0xff.ark.server.service;

import com.null0xff.ark.server.dto.GroupResponse;
import com.null0xff.ark.server.entity.Group;
import com.null0xff.ark.server.entity.GroupMember;
import com.null0xff.ark.server.entity.User;
import com.null0xff.ark.server.enums.GroupRole;
import com.null0xff.ark.server.exception.ResourceNotFoundException;
import com.null0xff.ark.server.repository.GroupMemberRepository;
import com.null0xff.ark.server.repository.GroupRepository;
import com.null0xff.ark.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing user profile information and account lifecycle.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;

    /**
     * Retrieves all groups that the specified user is a member of.
     *
     * @param userId The unique identifier of the user
     * @return A list of GroupResponse details
     */
    public List<GroupResponse> getUserGroups(UUID userId) {
        return groupMemberRepository.findByUserId(userId).stream()
                .map(membership -> {
                    Group group = membership.getGroup();
                    return new GroupResponse(group.getId(), group.getName(), group.getDescription(), group.getMaxPartiesPerCharacter());
                })
                .collect(Collectors.toList());
    }

    /**
     * Creates a new group and assigns the creator as the MANAGER.
     *
     * @param userId      The ID of the user creating the group
     * @param name        The name of the group
     * @param description Optional description
     * @return The newly created Group entity
     */
    @Transactional
    public Group createGroup(UUID userId, String name, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Group group = new Group();
        group.setName(name);
        group.setDescription(description);
        group = groupRepository.save(group);

        GroupMember membership = new GroupMember();
        membership.setGroup(group);
        membership.setUser(user);
        membership.setRole(GroupRole.MANAGER);
        groupMemberRepository.save(membership);

        return group;
    }

    /**
     * Updates the custom nickname for a user.
     *
     * @param userId   The unique identifier of the user
     * @param nickname The new nickname to set (can be null to revert to Discord username)
     */
    @Transactional
    public void updateNickname(UUID userId, String nickname) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setNickname(nickname);
        userRepository.save(user);
    }

    /**
     * Deletes a user account and all associated data from the system.
     *
     * @param userId The unique identifier of the user to delete
     */
    @Transactional
    public void deleteAccount(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found");
        }
        userRepository.deleteById(userId);
    }
}
