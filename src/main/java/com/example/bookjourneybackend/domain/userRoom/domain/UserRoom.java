package com.example.bookjourneybackend.domain.userRoom.domain;

import com.example.bookjourneybackend.domain.room.domain.Room;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.global.entity.BaseEntity;
import jakarta.persistence.*;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_room")
@NoArgsConstructor
@Getter
public class UserRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userRoomId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole userRole;

    @Setter
    @Column(nullable = false)
    private Double userPercentage;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer currentPage;

    @Setter
    private LocalDateTime inActivatedAt;

    @Setter
    private LocalDateTime completedUserPercentageAt;

    // Room의 관계 추가
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Builder
    public UserRoom(UserRole userRole, Double userPercentage, User user, Integer currentPage, Room room, LocalDateTime inActivatedAt) {
        this.userRole = userRole;
        this.userPercentage = userPercentage;
        this.user = user;
        this.currentPage = currentPage;
        this.room = room;
        this.inActivatedAt = inActivatedAt;
    }

    public void updateUserProgress(double percentage, int currentPage) {
        this.userPercentage = percentage;
        this.currentPage = currentPage;
    }

}
