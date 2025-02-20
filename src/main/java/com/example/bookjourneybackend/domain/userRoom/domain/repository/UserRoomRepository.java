package com.example.bookjourneybackend.domain.userRoom.domain.repository;

import com.example.bookjourneybackend.domain.room.domain.Room;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRoom;
import com.example.bookjourneybackend.global.entity.EntityStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRoomRepository extends JpaRepository<UserRoom, Long> {
    @Query("SELECT ur FROM UserRoom ur " +
            "JOIN FETCH ur.room r " +
            "WHERE ur.user.userId = :userId AND ur.status = 'ACTIVE'" +
            "ORDER BY (SELECT MAX(rec.modifiedAt) FROM Record rec WHERE rec.room = r) DESC ")
    List<UserRoom> findUserRoomsOrderByModifiedAt(@Param("userId") Long userId);

    @Query("SELECT ur FROM UserRoom ur " +
            "JOIN FETCH ur.room r " +
            "WHERE ur.user.userId = :userId AND ur.status = 'ACTIVE'" +
            "ORDER BY ur.userPercentage DESC")
    List<UserRoom> findUserRoomsOrderByUserPercentage(@Param("userId") Long userId);

    Optional<UserRoom> findUserRoomByRoomAndUser(Room room, User user);

    Optional<UserRoom> findUserRoomByRoomAndUserAndStatusNot(Room room, User user, EntityStatus status);

    boolean existsByRoomAndUser(Room room, User user);

    List<UserRoom> findAllByRoom(Room room);

   @Query("SELECT ur FROM UserRoom ur " +
            "JOIN FETCH ur.room r " +
            "WHERE ur.user.userId = :userId AND ur.status = :status " +
            "AND ((:month IS NULL OR MONTH(r.startDate) <= :month) AND YEAR(r.startDate) <= :year " +
            "AND (:month IS NULL OR MONTH(r.progressEndDate) >= :month) AND YEAR(r.progressEndDate) >= :year) " +
            "AND r.roomType = 'TOGETHER'")
   List<UserRoom> findInActiveTogetherRoomsByUserIdAndDate(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month, @Param("status") EntityStatus status);

     @Query("SELECT ur FROM UserRoom ur " +
            "JOIN FETCH ur.room r " +
            "WHERE ur.user.userId = :userId AND ur.status = :status " +
            "AND ((:month IS NULL OR MONTH(r.startDate) <= :month) AND YEAR(r.startDate) <= :year) " +
            "AND (r.progressEndDate IS NULL OR ((:month IS NULL OR MONTH(r.progressEndDate) >= :month) AND YEAR(r.progressEndDate) >= :year)) " +
            "AND r.roomType = 'ALONE'")
    List<UserRoom> findInActiveAloneRoomsByUserIdAndDate(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month, @Param("status") EntityStatus status);


    //해당 User가 Book으로 혼자읽기 방을 이미 만들었고 그 혼자읽기 방의 status가 EXPIRED가 아니라면 true
    @Query(
            "SELECT CASE WHEN COUNT(ur) > 0 THEN TRUE ELSE FALSE END " +
                    "FROM UserRoom ur " +
                    "LEFT JOIN ur.room r " +
                    "WHERE ur.user.userId = :userId AND r.book.isbn = :isbn AND r.roomType = 'ALONE' AND r.status != 'EXPIRED'"
    )
    boolean existsUnExpiredAloneRoomByUserAndBook(@Param("userId") Long userId, @Param("isbn") String isbn);

    @Query("SELECT ur FROM UserRoom ur " +
            "WHERE ur.user.userId = :userId AND ur.userPercentage >= 100 " +
            "AND MONTH(ur.completedUserPercentageAt) = :month AND YEAR(ur.completedUserPercentageAt) = :year " +
//            "AND ur.completedUserPercentageAt IN (" +
//            "    SELECT MIN(ur2.completedUserPercentageAt) " +
//            "    FROM UserRoom ur2 " +
//            "    WHERE ur2.user.userId = :userId AND ur2.userPercentage >= 100 " +
//            "    AND MONTH(ur2.completedUserPercentageAt) = :month AND YEAR(ur2.completedUserPercentageAt) = :year " +
//            "    GROUP BY DAY(ur2.completedUserPercentageAt)" +
//            ") " +"
            "ORDER BY DAY(ur.completedUserPercentageAt) ASC " +
            ",ur.completedUserPercentageAt ASC")
    List<UserRoom> findUserRoomsByUserInCalendar(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month);

    @Query("SELECT ur FROM UserRoom ur " +
            "WHERE ur.user.userId = :userId AND ur.userPercentage >= 100 " +
            "AND MONTH(ur.completedUserPercentageAt) = :month AND YEAR(ur.completedUserPercentageAt) = :year " +
            "AND DAY(ur.completedUserPercentageAt) = :date ORDER BY ur.completedUserPercentageAt ASC")
    List<UserRoom> findUserRoomsByUserInCalendarInfo(@Param("userId") Long userId, @Param("year") Integer year, @Param("month") Integer month, @Param("date") Integer date);
}
