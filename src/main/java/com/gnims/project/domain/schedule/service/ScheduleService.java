package com.gnims.project.domain.schedule.service;

import com.gnims.project.domain.event.entity.Event;
import com.gnims.project.domain.event.repository.EventRepository;
import com.gnims.project.domain.friendship.entity.Friendship;
import com.gnims.project.domain.friendship.repository.FriendshipRepository;
import com.gnims.project.domain.schedule.dto.*;
import com.gnims.project.domain.schedule.entity.Schedule;
import com.gnims.project.domain.schedule.repository.ScheduleRepository;
import com.gnims.project.domain.user.entity.User;
import com.gnims.project.domain.user.repository.UserRepository;
import com.gnims.project.share.persistence.embedded.Appointment;
import io.jsonwebtoken.security.SecurityException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.gnims.project.domain.schedule.entity.ScheduleStatus.*;
import static com.gnims.project.share.message.ExceptionMessage.*;
import static java.util.stream.Collectors.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;

    private static List<String> SORTING_GROUP = List.of("event.createAt", "event.dDay");

    public void makeSchedule(ScheduleServiceForm form) {
        //이벤트 엔티티 생성 및 저장
        Long userId = form.getCreateBy();
        Event event = eventRepository.save(new Event(new Appointment(form), form));
        //주최자 스케줄 처리 -> 주최자는 자동 일정에 자동 참여
        User user = userRepository.findById(userId).orElseThrow(
                () -> new IllegalArgumentException(NOT_EXISTED_USER));
        Schedule hostSchedule = new Schedule(user, event);
        hostSchedule.decideScheduleStatus(ACCEPT);
        //개인 스케줄일 경우
        if (isPersonalSchedule(form, userId)) {
            scheduleRepository.save(hostSchedule);
            return;
        }
        //초대된 사용자 목록
        List<User> users = userRepository.findAllById(form.getParticipantsId());
        //User 들이 팔로우 인지 확인할 필요성 있음 (개선 버전에서 추가)
        //스케쥴 엔티티 생성 및 저장
        List<Schedule> schedules = users.stream()
                //자기 자신이 participants 목록에 들어갈 경우 필터링
                .filter(u -> !userId.equals(u.getId()))
                .map(u -> new Schedule(u, event)).collect(toList());
        schedules.add(hostSchedule);
        scheduleRepository.saveAll(schedules);
    }

    public List<ReadAllResponse> readAllSchedule(Long myselfId, Long searchUserId) {
        // 스케줄 리스트 구하는 작업
        List<ReadAllScheduleDto> schedules = scheduleRepository.readAllSchedule(searchUserId);
        HashSet<Long> distinctContainer = new HashSet<>(schedules.size());
        List<ReadAllScheduleDto> notDuplicatedSchedules = schedules.stream() // 중복 제거 작업
                .filter(schedule -> distinctContainer.add(schedule.getEventId())).collect(toList());

        if (matched(myselfId, searchUserId)) { // [0] 내 스케줄 전체 조회
            return createReadAllResponse(schedules, notDuplicatedSchedules);
        }

        Optional<Friendship> friendship = friendshipRepository.readAllFollowingOf(myselfId) // [1] 팔로우 전체 스케줄 조회
                .stream().filter(f -> f.matchFollow(searchUserId)).findFirst();

        if (friendship.isEmpty()) {
            throw new SecurityException(FORBIDDEN);
        }

        return createReadAllResponse(schedules, notDuplicatedSchedules);
    }

    public PageableReadResponse readAllSchedulePage(Long userId, PageRequest pageRequest, String sorting) {
        if (!SORTING_GROUP.contains(sorting)) {
            throw new IllegalArgumentException(BAD_REQUEST);
        }
        // 이베트 생성순일때는 내림차순으로 변경
        if ("event.createAt".equals(sorting)) {
            pageRequest = pageRequest.withSort(pageRequest.getSort().descending());
        }

        Page<ReadAllScheduleDto> schedules = scheduleRepository.readAllSchedulePage(userId, pageRequest);

        List<ReadAllResponse> responses = schedules.stream().map(ds -> new ReadAllResponse(
                ds.getEventId(),
                ds.getDate(),
                ds.getTime(),
                ds.getCardColor(),
                ds.getSubject(),
                ds.getDDay(),
                fetchInvitee(schedules.stream(), ds))).collect(toList());

        return new PageableReadResponse(schedules.getTotalPages(), responses);
    }

    public ReadOneResponse readOneSchedule(Long myselfId, Long eventId) {
        List<ReadOneScheduleDto> schedules = scheduleRepository.readOneSchedule(eventId);
        if (schedules.isEmpty()) {
            throw new IndexOutOfBoundsException(NOT_EXISTED_SCHEDULE); // 존재하지 않은 일정을 검색할 경우
        }
        ReadOneScheduleDto schedule = schedules.get(0);

        if (schedule.isCreatedBy(myselfId)) { // [0] 내가 만든 일정
            return createReadOneResponse(schedules, schedule);
        }
        // [1] 내가 만든 일정도 아니고 참가자도 아니지만 상대방을 팔로우한 경우 | [2] 팔로우 하지 않았더라도 일정에 초대 받아 수락한 경우
        List<Long> myFollows = friendshipRepository.readAllFollowingOf(myselfId).stream() // (1) 내 팔로우 목록
                .map(f -> f.receiveFollowId())
                .collect(toList());

        List<Schedule> schedulesOfEvent = scheduleRepository.readAllByEventId(eventId); // (2) 이벤트를 참조 중인 스케줄 가져오기
        List<Long> participants = fetchEventParticipants(schedulesOfEvent); // (3) 일정 참여자 목록 가져오기

        if (matched(myselfId, participants)) { // [2] 팔로우 하지 않았더라도 일정에 초대 받아 수락한 경우
            return createReadOneResponse(schedules, schedule);
        }
        myFollows.stream()// (4) 참여자 중에 내 팔로우가 있는지 검증 - [1] 내가 만든 일정도 아니고 참가자도 아니지만 상대방을 팔로우
                .filter(f -> participants.contains(f)).findFirst()
                .orElseThrow(() -> new SecurityException(FORBIDDEN)); // [1], [2] 에도 해당하지 않으면 접근 권한 X
        return createReadOneResponse(schedules, schedule);
    }

    public List<ReadPendingResponse> readPendingSchedule(Long userId) {
        List<ReadPendingDto> schedules = scheduleRepository.readAllPendingSchedule(userId);

        return schedules.stream().map(s -> new ReadPendingResponse(
                s.getEvent_Id(),
                s.getUsername(),
                s.getDate(),
                s.getTime(),
                s.getCard_Color(),
                s.getSubject())).collect(toList());
    }

    public List<ReadAllResponse> readPastSchedule(Long userId) {
        List<ReadAllScheduleDto> schedules = scheduleRepository.readPastSchedule(userId);

        HashSet<Long> set = new HashSet<>(schedules.size());
        List<ReadAllScheduleDto> distinctSchedule = schedules.stream()
                .filter(e -> set.add(e.getEventId())).collect(toList());

        return distinctSchedule.stream().map(ds -> new ReadAllResponse(
                ds.getEventId(),
                ds.getDate(),
                ds.getTime(),
                ds.getCardColor(),
                ds.getSubject(),
                ds.getDDay(),
                fetchInvitee(schedules.stream(), ds))).collect(toList());
    }

    @Transactional
    public void acceptSchedule(Long userId, Long eventId) {
        Schedule schedule = scheduleRepository.readOnePendingSchedule(userId, eventId)
                .orElseThrow(() -> new IllegalArgumentException(ALREADY_PROCESSED_OR_NOT_EXISTED_SCHEDULE));

        schedule.decideScheduleStatus(ACCEPT);
        scheduleRepository.save(schedule);
    }

    @Transactional
    public void rejectSchedule(Long userId, Long eventId) {
        Schedule schedule = scheduleRepository.readOnePendingSchedule(userId, eventId)
                .orElseThrow(() -> new IllegalArgumentException(ALREADY_PROCESSED_OR_NOT_EXISTED_SCHEDULE));

        schedule.decideScheduleStatus(REJECT);
        scheduleRepository.save(schedule);
    }

    private boolean isPersonalSchedule(ScheduleServiceForm form, Long userId) {
        return form.getParticipantsId().isEmpty() ||
                (form.getParticipantsId().size() == 1 && form.getParticipantsId().get(0).equals(userId));
    }

    private List<ReadAllResponse> createReadAllResponse(List<ReadAllScheduleDto> inviteeOfSchedule, List<ReadAllScheduleDto> schedules) {
        return schedules.stream().map(schedule -> new ReadAllResponse(
                schedule.getEventId(),
                schedule.getDate(),
                schedule.getTime(),
                schedule.getCardColor(),
                schedule.getSubject(),
                schedule.getDDay(),
                fetchInvitee(inviteeOfSchedule.stream(), schedule))).collect(toList());
    }

    private List<ReadAllUserDto> fetchInvitee(Stream<ReadAllScheduleDto> eventAllQueries, ReadAllScheduleDto ds) {
        return eventAllQueries
                .filter(eq -> eq.isSameEventId(ds.getEventId()))
                .map(eq -> new ReadAllUserDto(eq.getUsername(), eq.getProfile()))
                .collect(toList());
    }

    private ReadOneResponse createReadOneResponse(List<ReadOneScheduleDto> schedules, ReadOneScheduleDto schedule) {
        return new ReadOneResponse(
                schedule.getEventId(),
                schedule.getDate(),
                schedule.getTime(),
                schedule.getCardColor(),
                schedule.getSubject(),
                schedule.getContent(),
                schedule.getHostId(),
                schedule.getDDay(),
                schedules.stream().map(s -> new ReadOneUserDto(s.getUsername())).collect(toList())
        );
    }

    private boolean matched(Long myselfId, List<Long> participants) {
        return participants.stream().filter(p -> p.equals(myselfId)).findFirst().isPresent();
    }

    private boolean matched(Long myselfId, Long searchUserId) {
        return searchUserId.equals(myselfId);
    }

    private List<Long> fetchEventParticipants(List<Schedule> schedulesOfEvent) {
        List<Long> participants = schedulesOfEvent.stream()
                .filter(s -> s.isAccepted())
                .filter(s -> s.getEvent().isNotDeleted())
                .filter(s -> s.getEvent().isNotPast())
                .map(s -> s.getUser().getId()).collect(toList());
        return participants;
    }
}
