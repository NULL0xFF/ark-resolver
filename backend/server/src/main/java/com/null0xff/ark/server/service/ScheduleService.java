package com.null0xff.ark.server.service;

import com.null0xff.ark.server.dto.ScheduleResponse;
import com.null0xff.ark.server.entity.GroupMember;
import com.null0xff.ark.server.entity.ScheduleInstance;
import com.null0xff.ark.server.enums.GroupRole;
import com.null0xff.ark.server.exception.ForbiddenException;
import com.null0xff.ark.server.exception.ResourceNotFoundException;
import com.null0xff.ark.server.exception.ValidationException;
import com.null0xff.ark.server.repository.GroupMemberRepository;
import com.null0xff.ark.server.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing schedule instances within groups.
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final AuthorizationHelper authorizationHelper;

    @Transactional
    public ScheduleResponse createSchedule(UUID groupId, UUID userId, String title, Instant start, Instant end) {
        GroupMember member = authorizationHelper.requireRole(groupId, userId, GroupRole.MANAGER, GroupRole.AUDITOR);

        if (start == null || end == null || !end.isAfter(start)) {
            throw new ValidationException("Invalid schedule time range", groupId);
        }

        ScheduleInstance schedule = new ScheduleInstance();
        schedule.setGroup(member.getGroup());
        schedule.setTitle(title);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        scheduleRepository.save(schedule);

        return new ScheduleResponse(schedule.getId(), schedule.getTitle(), schedule.getStartTime(), schedule.getEndTime(), schedule.getGroup().getId());
    }

    public List<ScheduleResponse> getGroupSchedules(UUID groupId, UUID userId) {
        authorizationHelper.requireMembership(groupId, userId);

        return scheduleRepository.findByGroupIdAndArchivedFalseOrderByStartTimeAsc(groupId).stream()
                .map(s -> new ScheduleResponse(s.getId(), s.getTitle(), s.getStartTime(), s.getEndTime(), s.getGroup().getId()))
                .collect(Collectors.toList());
    }

    public List<ScheduleResponse> getArchivedSchedules(UUID groupId, UUID userId) {
        authorizationHelper.requireMembership(groupId, userId);

        return scheduleRepository.findByGroupIdAndArchivedTrueOrderByStartTimeDesc(groupId).stream()
                .map(s -> new ScheduleResponse(s.getId(), s.getTitle(), s.getStartTime(), s.getEndTime(), s.getGroup().getId()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void archiveSchedule(UUID scheduleId, UUID userId) {
        ScheduleInstance schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found", scheduleId));

        authorizationHelper.requireRole(schedule.getGroup().getId(), userId, GroupRole.MANAGER, GroupRole.AUDITOR);

        schedule.setArchived(true);
        scheduleRepository.save(schedule);
    }

    @Transactional
    public void unarchiveSchedule(UUID scheduleId, UUID userId) {
        ScheduleInstance schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found", scheduleId));

        authorizationHelper.requireRole(schedule.getGroup().getId(), userId, GroupRole.MANAGER, GroupRole.AUDITOR);

        schedule.setArchived(false);
        scheduleRepository.save(schedule);
    }

    public ScheduleResponse getSchedule(UUID scheduleId, UUID userId) {
        ScheduleInstance schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found", scheduleId));

        authorizationHelper.requireMembership(schedule.getGroup().getId(), userId);

        return new ScheduleResponse(schedule.getId(), schedule.getTitle(), schedule.getStartTime(), schedule.getEndTime(), schedule.getGroup().getId());
    }

    @Transactional
    public void updateSchedule(UUID scheduleId, UUID userId, String title, Instant start, Instant end) {
        ScheduleInstance schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found", scheduleId));

        authorizationHelper.requireRole(schedule.getGroup().getId(), userId, GroupRole.MANAGER, GroupRole.AUDITOR);

        if (start == null || end == null || !end.isAfter(start)) {
            throw new ValidationException("Invalid schedule time range", scheduleId);
        }

        schedule.setTitle(title);
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        scheduleRepository.save(schedule);
    }

    @Transactional
    public void deleteSchedule(UUID scheduleId, UUID userId) {
        ScheduleInstance schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found", scheduleId));

        authorizationHelper.requireRole(schedule.getGroup().getId(), userId, GroupRole.MANAGER, GroupRole.AUDITOR);

        scheduleRepository.delete(schedule);
    }
}
