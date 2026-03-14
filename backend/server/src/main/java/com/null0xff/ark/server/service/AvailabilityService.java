package com.null0xff.ark.server.service;

import com.null0xff.ark.server.dto.AvailabilityBlock;
import com.null0xff.ark.server.dto.AvailabilityResponse;
import com.null0xff.ark.server.entity.GroupMember;
import com.null0xff.ark.server.entity.MemberAvailability;
import com.null0xff.ark.server.entity.ScheduleInstance;
import com.null0xff.ark.server.entity.User;
import com.null0xff.ark.server.exception.ForbiddenException;
import com.null0xff.ark.server.exception.ResourceNotFoundException;
import com.null0xff.ark.server.exception.ValidationException;
import com.null0xff.ark.server.repository.GroupMemberRepository;
import com.null0xff.ark.server.repository.MemberAvailabilityRepository;
import com.null0xff.ark.server.repository.ScheduleRepository;
import com.null0xff.ark.server.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing member availability and aggregated heatmaps.
 */
@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private final MemberAvailabilityRepository availabilityRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final GroupMemberRepository groupMemberRepository;

    @Transactional
    public void updateAvailability(UUID scheduleId, UUID userId, List<AvailabilityBlock> blocks) {
        ScheduleInstance schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found", scheduleId));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found", userId));

        groupMemberRepository.findByGroupIdAndUserId(schedule.getGroup().getId(), userId)
                .orElseThrow(() -> new ForbiddenException("Access denied", scheduleId));

        // Server-side validation: Ensure all blocks are within schedule range
        for (AvailabilityBlock block : blocks) {
            if (block.getStart().isBefore(schedule.getStartTime()) || 
                block.getEnd().isAfter(schedule.getEndTime())) {
                throw new ValidationException("Availability block outside of schedule range", scheduleId);
            }
        }

        availabilityRepository.deleteByScheduleIdAndUserId(scheduleId, userId);

        List<MemberAvailability> newAvailabilities = blocks.stream().map(block -> {
            MemberAvailability availability = new MemberAvailability();
            availability.setSchedule(schedule);
            availability.setUser(user);
            availability.setStartTime(block.getStart());
            availability.setEndTime(block.getEnd());
            return availability;
        }).collect(Collectors.toList());

        availabilityRepository.saveAll(newAvailabilities);
    }

    public AvailabilityResponse getUserAvailability(UUID scheduleId, UUID userId) {
        ScheduleInstance schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found", scheduleId));

        groupMemberRepository.findByGroupIdAndUserId(schedule.getGroup().getId(), userId)
                .orElseThrow(() -> new ForbiddenException("Access denied", scheduleId));

        List<AvailabilityBlock> blocks = availabilityRepository.findByScheduleIdAndUserId(scheduleId, userId).stream()
                .map(a -> {
                    AvailabilityBlock block = new AvailabilityBlock();
                    block.setStart(a.getStartTime());
                    block.setEnd(a.getEndTime());
                    return block;
                })
                .collect(Collectors.toList());

        return new AvailabilityResponse(userId, blocks);
    }

    public List<AvailabilityResponse> getAggregatedAvailability(UUID scheduleId, UUID userId) {
        ScheduleInstance schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Schedule not found", scheduleId));

        groupMemberRepository.findByGroupIdAndUserId(schedule.getGroup().getId(), userId)
                .orElseThrow(() -> new ForbiddenException("Access denied", scheduleId));

        List<MemberAvailability> availabilities = availabilityRepository.findByScheduleId(scheduleId);

        Map<UUID, List<MemberAvailability>> byUser = availabilities.stream()
                .collect(Collectors.groupingBy(a -> a.getUser().getId()));

        return byUser.entrySet().stream()
                .map(entry -> {
                    List<AvailabilityBlock> blocks = entry.getValue().stream().map(a -> {
                        AvailabilityBlock block = new AvailabilityBlock();
                        block.setStart(a.getStartTime());
                        block.setEnd(a.getEndTime());
                        return block;
                    }).collect(Collectors.toList());
                    return new AvailabilityResponse(entry.getKey(), blocks);
                })
                .collect(Collectors.toList());
    }
}
