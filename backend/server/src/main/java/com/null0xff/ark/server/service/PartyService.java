package com.null0xff.ark.server.service;

import com.null0xff.ark.server.dto.PartyMemberResponse;
import com.null0xff.ark.server.dto.PartyRequest;
import com.null0xff.ark.server.dto.PartyResponse;
import com.null0xff.ark.server.entity.GroupCharacter;
import com.null0xff.ark.server.entity.GroupMember;
import com.null0xff.ark.server.entity.Party;
import com.null0xff.ark.server.entity.PartyMember;
import com.null0xff.ark.server.entity.ScheduleInstance;
import com.null0xff.ark.server.enums.GroupRole;
import com.null0xff.ark.server.exception.ForbiddenException;
import com.null0xff.ark.server.exception.ResourceNotFoundException;
import com.null0xff.ark.server.exception.ValidationException;
import com.null0xff.ark.server.repository.GroupCharacterRepository;
import com.null0xff.ark.server.repository.GroupMemberRepository;
import com.null0xff.ark.server.repository.PartyMemberRepository;
import com.null0xff.ark.server.repository.PartyRepository;
import com.null0xff.ark.server.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing raid parties within schedules.
 */
@Service
@RequiredArgsConstructor
public class PartyService {

  private final PartyRepository partyRepository;
  private final PartyMemberRepository partyMemberRepository;
  private final ScheduleRepository scheduleRepository;
  private final GroupMemberRepository groupMemberRepository;
  private final GroupCharacterRepository characterRepository;

  @Transactional
  public PartyResponse createParty(UUID scheduleId, UUID managerId, PartyRequest request) {
    ScheduleInstance schedule = scheduleRepository.findById(scheduleId)
        .orElseThrow(() -> new ResourceNotFoundException("Schedule not found", scheduleId));

    GroupMember manager = groupMemberRepository.findByGroupIdAndUserId(schedule.getGroup().getId(), managerId)
        .orElseThrow(() -> new ForbiddenException("Access denied", scheduleId));

    if (manager.getRole() != GroupRole.MANAGER && manager.getRole() != GroupRole.AUDITOR) {
      throw new ForbiddenException("Insufficient permissions to create parties", scheduleId);
    }

    Party party = new Party();
    party.setSchedule(schedule);
    party.setTitle(request.getTitle());
    party.setRaidType(request.getRaidType());
    party.setMaxMembers(request.getMaxMembers());
    party.setStartTime(request.getStartTime());

    partyRepository.save(party);

    return new PartyResponse(party.getId(), scheduleId, party.getTitle(), party.getRaidType(),
        0, party.getMaxMembers(), party.getStartTime(), party.getIsCompleted(), List.of());
  }

  public List<PartyResponse> getScheduleParties(UUID scheduleId, UUID userId) {
    ScheduleInstance schedule = scheduleRepository.findById(scheduleId)
        .orElseThrow(() -> new ResourceNotFoundException("Schedule not found", scheduleId));

    groupMemberRepository.findByGroupIdAndUserId(schedule.getGroup().getId(), userId)
        .orElseThrow(() -> new ForbiddenException("Access denied", scheduleId));

    return partyRepository.findByScheduleIdOrderByStartTimeAscTitleAsc(scheduleId).stream().map(party -> {
      List<PartyMember> slots = partyMemberRepository.findByPartyId(party.getId());
      List<PartyMemberResponse> memberResponses = slots.stream()
          .map(pm -> new PartyMemberResponse(
              pm.getCharacter().getId(),
              pm.getCharacter().getName(),
              pm.getCharacter().getCharacterClass(),
              pm.getCharacter().getItemLevel(),
              pm.getCharacter().getUser().getId()))
          .collect(Collectors.toList());
      return new PartyResponse(party.getId(), scheduleId, party.getTitle(), party.getRaidType(),
          slots.size(), party.getMaxMembers(), party.getStartTime(), party.getIsCompleted(), memberResponses);
    }).collect(Collectors.toList());
  }

  @Transactional
  public void joinParty(UUID partyId, UUID characterId, UUID userId) {
    Party party = partyRepository.findById(partyId)
        .orElseThrow(() -> new ResourceNotFoundException("Party not found", partyId));

    UUID groupId = party.getSchedule().getGroup().getId();
    groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
        .orElseThrow(() -> new ForbiddenException("Access denied", partyId));

    GroupCharacter character = characterRepository.findByIdAndGroupIdAndUserId(characterId, groupId, userId)
        .orElseThrow(() -> new ResourceNotFoundException("Character not found in this group", characterId));

    if (partyMemberRepository.findByPartyIdAndCharacterId(partyId, characterId).isPresent()) {
      throw new ValidationException("This character is already in the party", partyId);
    }

    int currentSlots = partyMemberRepository.countByPartyId(partyId);
    if (currentSlots >= party.getMaxMembers()) {
      throw new ValidationException("Party is full", partyId);
    }

    int limit = party.getSchedule().getGroup().getMaxPartiesPerCharacter();
    int currentParties = partyMemberRepository.countByCharacterIdAndPartyScheduleId(characterId, party.getSchedule().getId());
    if (currentParties >= limit) {
      throw new ValidationException(
          "Character has reached the maximum of " + limit + " parties per schedule", partyId);
    }

    PartyMember slot = new PartyMember();
    slot.setParty(party);
    slot.setCharacter(character);
    partyMemberRepository.save(slot);
  }

  @Transactional
  public void leaveParty(UUID partyId, UUID characterId, UUID userId) {
    Party party = partyRepository.findById(partyId)
        .orElseThrow(() -> new ResourceNotFoundException("Party not found", partyId));

    UUID groupId = party.getSchedule().getGroup().getId();
    GroupMember requester = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
        .orElseThrow(() -> new ForbiddenException("Access denied", partyId));

    boolean isAdmin = requester.getRole() == GroupRole.MANAGER || requester.getRole() == GroupRole.AUDITOR;
    if (!isAdmin) {
      characterRepository.findByIdAndGroupIdAndUserId(characterId, groupId, userId)
          .orElseThrow(() -> new ForbiddenException("You do not own this character", partyId));
    }

    PartyMember slot = partyMemberRepository.findByPartyIdAndCharacterId(partyId, characterId)
        .orElseThrow(() -> new ValidationException("This character is not in the party", partyId));

    partyMemberRepository.delete(slot);
  }

  @Transactional
  public void deleteParty(UUID partyId, UUID managerId) {
    Party party = partyRepository.findById(partyId)
        .orElseThrow(() -> new ResourceNotFoundException("Party not found", partyId));

    GroupMember manager = groupMemberRepository.findByGroupIdAndUserId(party.getSchedule().getGroup().getId(), managerId)
        .orElseThrow(() -> new ForbiddenException("Access denied", partyId));

    if (manager.getRole() != GroupRole.MANAGER && manager.getRole() != GroupRole.AUDITOR) {
      throw new ForbiddenException("Insufficient permissions to delete parties", partyId);
    }

    partyRepository.delete(party);
  }

  @Transactional
  public void updateParty(UUID partyId, UUID managerId, String title, String raidType) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("Party title must not be blank");
    }

    Party party = partyRepository.findById(partyId)
        .orElseThrow(() -> new ResourceNotFoundException("Party not found", partyId));

    GroupMember manager = groupMemberRepository.findByGroupIdAndUserId(party.getSchedule().getGroup().getId(), managerId)
        .orElseThrow(() -> new ForbiddenException("Access denied", partyId));

    if (manager.getRole() != GroupRole.MANAGER && manager.getRole() != GroupRole.AUDITOR) {
      throw new ForbiddenException("Insufficient permissions to update parties", partyId);
    }

    party.setTitle(title.strip());
    if (raidType != null && !raidType.isBlank()) {
      party.setRaidType(raidType.strip());
    }
    partyRepository.save(party);
  }

  @Transactional
  public void markPartyAsDone(UUID partyId, UUID managerId, boolean completed) {
    Party party = partyRepository.findById(partyId)
        .orElseThrow(() -> new ResourceNotFoundException("Party not found", partyId));

    GroupMember manager = groupMemberRepository.findByGroupIdAndUserId(party.getSchedule().getGroup().getId(), managerId)
        .orElseThrow(() -> new ForbiddenException("Access denied", partyId));

    if (manager.getRole() != GroupRole.MANAGER && manager.getRole() != GroupRole.AUDITOR) {
      throw new ForbiddenException("Insufficient permissions to mark parties as done", partyId);
    }

    party.setIsCompleted(completed);
    partyRepository.save(party);
  }
}
