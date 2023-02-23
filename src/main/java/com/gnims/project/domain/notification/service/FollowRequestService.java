package com.gnims.project.domain.notification.service;

import com.gnims.project.domain.notification.entity.FollowRequest;
import com.gnims.project.domain.notification.repository.FollowRequestRepository;
import com.gnims.project.domain.user.entity.User;
import com.gnims.project.domain.user.repository.UserRepository;
import com.gnims.project.domain.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class FollowRequestService {
    @Autowired
    FollowRequestRepository followRequestRepository;
    @Autowired
    UserService userService;
    @Autowired
    private UserRepository userRepository;

    public int request_save(Long requestid, Long receiveid) { // 요청 저장하기

        User requestuser = userRepository.findById(requestid).get();
        User receiveuser = userRepository.findById(receiveid).get();
        FollowRequest fr = new FollowRequest(requestuser,receiveuser);
        followRequestRepository.save(fr);
        return 1;
    }

    public boolean request(int requstid, int receiveid) {
        // 요청이있으면 true반환
        if(followRequestRepository.countByRequestIdAndReceiveId(requstid, receiveid) == 1)
            return true;
        else
            return false;
    }
}