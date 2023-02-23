package com.gnims.project.domain.notification.repository;


import com.gnims.project.domain.notification.entity.FollowRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowRequestRepository extends JpaRepository<FollowRequest, Integer> {
    // 요청user의 id와 요청받는user의 id가 id1, id2인 개수
    int countByRequestIdAndReceiveId(int id1, int id2);
}