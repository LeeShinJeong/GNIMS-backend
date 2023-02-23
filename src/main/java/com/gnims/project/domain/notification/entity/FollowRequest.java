package com.gnims.project.domain.notification.entity;

import com.gnims.project.domain.user.entity.User;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Getter
@NoArgsConstructor
@Entity
@org.hibernate.annotations.DynamicUpdate
public class FollowRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    int id;

    @ManyToOne
    @JoinColumn(name = "requestuser")
    User request;

    @ManyToOne
    @JoinColumn(name = "receiveuser")
    User receive;

    public FollowRequest(User request, User receive) {
        this.request = request;
        this.receive = receive;
    }
}