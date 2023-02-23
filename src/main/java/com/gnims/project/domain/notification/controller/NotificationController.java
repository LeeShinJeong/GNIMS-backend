package com.gnims.project.domain.notification.controller;

import com.gnims.project.domain.friendship.dto.FollowReadResponse;
import com.gnims.project.domain.notification.dto.NotificationCountDto;
import com.gnims.project.domain.notification.dto.NotificationDto;
import com.gnims.project.domain.notification.entity.FollowRequest;
import com.gnims.project.domain.notification.entity.Notification;
import com.gnims.project.domain.notification.service.NotificationService;
import com.gnims.project.domain.notification.repository.SseEmitterManager;
import com.gnims.project.domain.schedule.dto.ScheduleServiceForm;
import com.gnims.project.exception.advice.StatusResponseDto;
import com.gnims.project.security.service.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@Slf4j
@RestController
@RequiredArgsConstructor
@Transactional
public class NotificationController {
//    private final SseEmitterManager sseEmitterManager;
//    private final NotificationService notificationService;
//
//    @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
//    public SseEmitter connect(@AuthenticationPrincipal UserDetailsImpl userDetails) {
//        SseEmitter sseEmitter = sseEmitterManager.save(userDetails.receiveUserId());
//        sseEmitterManager.sendInitMessage(sseEmitter, userDetails.getUsername());
//
//        return sseEmitter;
//    }



    /**
     * 비동기 더 공부해야되요. 쓰레드 풀이란걸 통해 관리해야 된다고 하네요?
     */
//    @Async
//    @EventListener
//    public void helloPush(ScheduleServiceForm form) {
//        log.info("이벤트 리스너의 살기 감지!");
//        List<Long> participantsIds = form.getParticipantsId();
//        sendScheduleAlarm(form, participantsIds);
//    }
//
//    @Async
//    @EventListener
//    public void twoPush(FollowRequest follow){
//        log.info("이벤트 리스너 2");
//        Long followId = follow.getReceive().getId();
//        sendFollowAlarm(follow, followId);
//    }
//
//
//    private void sendScheduleAlarm(ScheduleServiceForm form, List<Long> participantsIds) {
//
//        for (Long participantsId : participantsIds) {
//            Map<Long, SseEmitter> sseEmitters = sseEmitterManager.getSseEmitters();
//            SseEmitter sseEmitter = sseEmitters.get(participantsId);
//            try {
//                log.info("이벤트 리스너 {}, {}", form.getSubject(), form.getParticipantsId());
//
//                String message = form.getUsername() + "님께서 " + form.getSubject() + " 일정에 초대하셨습니다.";
//
//                Notification notification = notificationService.create(form.getId(), participantsId, message);
//
//                sseEmitter.send(SseEmitter.event()
//                        .name("invite")
//                        .data(notification, MediaType.APPLICATION_JSON));
//
//            } catch (IOException e) {
//                log.info("IO exception");
//            } catch (NullPointerException e) {
//                log.info("현재 {} 사용자는 알람을 사용하고 있지 않습니다.", participantsId);
//            } catch (IllegalStateException e) {
//                log.info("현재 {} 사용자의 Emitter는 꺼져있습니다.", participantsId);
//            }
//        }
//    }
//
//    private void sendFollowAlarm(FollowRequest follow, Long followId) {
//        SseEmitter sseEmitter = new SseEmitter();
//        //sseEmitter = sseEmitter.get(followId);
//            try {
//                log.info("이벤트 리스너 {}", followId);
//
//                String message = follow.getRequest().getUsername() + "님께서 팔로우 하셨습니다.";
//
//                Notification notification = notificationService.create(follow.getRequest().getId(), followId, message);
//
//                sseEmitter.send(SseEmitter.event()
//                        .name("follow")
//                        .data(notification, MediaType.APPLICATION_JSON));
//
//            } catch (IOException e) {
//                log.info("IO exception");
//            } catch (NullPointerException e) {
//                log.info("현재 {} 사용자는 알람을 사용하고 있지 않습니다.", followId);
//            } catch (IllegalStateException e) {
//                log.info("현재 {} 사용자의 Emitter는 꺼져있습니다.", followId);
//            }
//        }

    private final NotificationService notificationService;
    @GetMapping(value ="/subscribe" , produces = "text/event-stream")
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                @RequestHeader(value="Last-Event-ID",required = false,defaultValue = "")
                                String lastEventId){

        return notificationService.subscribe(userDetails.getUser().getId(),lastEventId);
    }
    //알림조회
    @GetMapping(value = "/notifications")
    public List<NotificationDto> findAllNotifications(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return notificationService.findAllNotifications(userDetails.getUser().getId());
    }

    //전체목록 알림 조회에서 해당 목록 클릭 시 읽음처리 ,
    @PostMapping("/notification/read/{notificationId}")
    public void readNotification(@PathVariable Long notificationId){
        notificationService.readNotification(notificationId);

    }
    //알림 조회 - 구독자가 현재 읽지않은 알림 갯수
    @GetMapping(value = "/notifications/count")
    public NotificationCountDto countUnReadNotifications(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        return notificationService.countUnReadNotifications(userDetails.getUser().getId());
    }

    //알림 전체 삭제
    @DeleteMapping(value = "/notifications/delete")
    public ResponseEntity<Object> deleteNotifications(@AuthenticationPrincipal UserDetailsImpl userDetails){

        notificationService.deleteAllByNotifications(userDetails);
        return new ResponseEntity<>(new StatusResponseDto("알림 목록 전체삭제 성공",""), HttpStatus.OK);
    }
    //단일 알림 삭제
    @DeleteMapping(value = "/notifications/delete/{notificationId}")
    public ResponseEntity<Object> deleteNotification(@PathVariable Long notificationId){

        notificationService.deleteByNotifications(notificationId);
        return new ResponseEntity<>(new StatusResponseDto("알림 목록 삭제 성공",""), HttpStatus.OK);
    }

}


