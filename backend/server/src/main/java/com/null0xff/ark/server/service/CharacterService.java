package com.null0xff.ark.server.service;

import com.null0xff.ark.server.dto.CharacterRequest;
import com.null0xff.ark.server.dto.CharacterResponse;
import com.null0xff.ark.server.entity.Group;
import com.null0xff.ark.server.entity.GroupCharacter;
import com.null0xff.ark.server.entity.User;
import com.null0xff.ark.server.exception.ResourceNotFoundException;
import com.null0xff.ark.server.repository.GroupCharacterRepository;
import com.null0xff.ark.server.repository.GroupRepository;
import com.null0xff.ark.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing group-scoped in-game characters.
 */
@Service
@RequiredArgsConstructor
public class CharacterService {

  private final GroupCharacterRepository characterRepository;
  private final GroupRepository groupRepository;
  private final UserRepository userRepository;
  private final AuthorizationHelper authorizationHelper;

  /**
   * Returns all characters registered by the calling user in a specific group.
   *
   * @param groupId the group's unique identifier
   * @param userId  the caller's unique identifier
   * @return a list of the user's characters; empty if none registered
   */
  public List<CharacterResponse> getMyCharacters(UUID groupId, UUID userId) {
    authorizationHelper.requireMembership(groupId, userId);
    return characterRepository.findByGroupIdAndUserId(groupId, userId).stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  /**
   * Returns all characters registered by all members in a specific group.
   *
   * @param groupId the group's unique identifier
   * @param userId  the caller's unique identifier (must be a member)
   * @return a list of all characters in the group
   */
  public List<CharacterResponse> getGroupCharacters(UUID groupId, UUID userId) {
    authorizationHelper.requireMembership(groupId, userId);

    Group group = groupRepository.findById(groupId)
        .orElseThrow(() -> new ResourceNotFoundException("Group not found", groupId));

    return group.getCharacters().stream()
        .map(this::toResponse)
        .collect(Collectors.toList());
  }

  /**
   * Registers a new character for the calling user in a group.
   *
   * @param userId  the caller's unique identifier
   * @param request the character details
   * @return the created character
   */
  @Transactional
  public CharacterResponse createCharacter(UUID userId, CharacterRequest request) {
    authorizationHelper.requireMembership(request.getGroupId(), userId);

    Group group = groupRepository.findById(request.getGroupId())
        .orElseThrow(() -> new ResourceNotFoundException("Group not found", request.getGroupId()));
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found", userId));

    if (request.getName() == null || request.getName().isBlank()) {
      throw new IllegalArgumentException("Character name must not be blank");
    }

    GroupCharacter character = new GroupCharacter();
    character.setGroup(group);
    character.setUser(user);
    character.setName(request.getName().strip());
    character.setCharacterClass(request.getCharacterClass());
    character.setItemLevel(request.getItemLevel());
    characterRepository.save(character);

    return toResponse(character);
  }

  /**
   * Updates an existing character owned by the calling user.
   *
   * @param userId  the caller's unique identifier
   * @param request the updated character details; must include {@code characterId}
   * @return the updated character
   */
  @Transactional
  public CharacterResponse updateCharacter(UUID userId, CharacterRequest request) {
    authorizationHelper.requireMembership(request.getGroupId(), userId);

    GroupCharacter character = characterRepository
        .findByIdAndGroupIdAndUserId(request.getCharacterId(), request.getGroupId(), userId)
        .orElseThrow(() -> new ResourceNotFoundException("Character not found", request.getCharacterId()));

    if (request.getName() == null || request.getName().isBlank()) {
      throw new IllegalArgumentException("Character name must not be blank");
    }
    character.setName(request.getName().strip());
    character.setCharacterClass(request.getCharacterClass());
    character.setItemLevel(request.getItemLevel());
    characterRepository.save(character);

    return toResponse(character);
  }

  /**
   * Deletes a character owned by the calling user.
   *
   * @param groupId     the group's unique identifier
   * @param characterId the character's unique identifier
   * @param userId      the caller's unique identifier
   */
  @Transactional
  public void deleteCharacter(UUID groupId, UUID characterId, UUID userId) {
    authorizationHelper.requireMembership(groupId, userId);

    GroupCharacter character = characterRepository
        .findByIdAndGroupIdAndUserId(characterId, groupId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Character not found", characterId));

    characterRepository.delete(character);
  }

  private CharacterResponse toResponse(GroupCharacter c) {
    return new CharacterResponse(c.getId(), c.getUser().getId(), c.getName(), c.getCharacterClass(), c.getItemLevel());
  }
}
