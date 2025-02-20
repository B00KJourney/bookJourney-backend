package com.example.bookjourneybackend.domain.userRoom.domain.repository;

import com.example.bookjourneybackend.domain.book.domain.Book;
import com.example.bookjourneybackend.domain.book.domain.repository.BookRepository;
import com.example.bookjourneybackend.domain.room.domain.Room;

import com.example.bookjourneybackend.domain.room.domain.repository.RoomRepository;
import com.example.bookjourneybackend.domain.user.domain.User;

import com.example.bookjourneybackend.domain.user.domain.repository.UserRepository;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRoom;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRole;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static com.example.bookjourneybackend.domain.book.domain.GenreType.NOVEL_POETRY_DRAMA;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
class UserRoomRepositoryTest {

    @Autowired
    private UserRoomRepository userRoomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RoomRepository roomRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("UserRoom 저장 및 연관관계 테스트")
    void saveUserRoomTest() {
        String email = "user@example.com";
        String roomName = "독서 모임";

        // given
        User user = userRepository.save(User.builder()
                .email(email)
                .password("verysecret123")
                .nickname("user123")
                .imageUrl("profile.jpg")
                .build());

        Book book = bookRepository.save(Book.builder()
                .bookTitle("테스트 책")
                .isbn("1234567890123")
                .authorName("테스트 저자")
                .genre(NOVEL_POETRY_DRAMA)
                .imageUrl("test.jpg")
                .bestSeller(false)
                .build());

        Room room = roomRepository.save(Room.makeReadTogetherRoom(
                roomName,
                book,
                true,
                1234,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(5),
                5
        ));

        UserRoom userRoom = UserRoom.builder()
                .userRole(UserRole.MEMBER)
                .userPercentage(0.0)
                .user(user)
                .currentPage(1)
                .room(room)
                .build();

        // When
        UserRoom savedUserRoom = userRoomRepository.save(userRoom);
        entityManager.flush();
        entityManager.clear();

        // Then
        UserRoom foundUserRoom = userRoomRepository.findById(savedUserRoom.getUserRoomId()).orElseThrow();
        assertThat(foundUserRoom).isNotNull();
        assertThat(foundUserRoom.getUser().getEmail()).isEqualTo(email);
        assertThat(foundUserRoom.getRoom().getRoomName()).isEqualTo(roomName);
        assertThat(foundUserRoom.getUserRole()).isEqualTo(UserRole.MEMBER);
    }

    @Test
    @DisplayName("UserRoom 진행률 업데이트 테스트")
    void updateUserProgressTest() {
        String email = "user@example.com";
        String roomName = "독서 방 이름";

        // given
        User user = userRepository.save(User.builder()
                .email(email)
                .password("verysecret123")
                .nickname("user123")
                .imageUrl("profile.jpg")
                .build());

        Book book = bookRepository.save(Book.builder()
                .bookTitle("테스트 책")
                .isbn("1234567890123")
                .authorName("테스트 저자")
                .genre(NOVEL_POETRY_DRAMA)
                .imageUrl("test.jpg")
                .bestSeller(false)
                .build());

        Room room = roomRepository.save(Room.makeReadTogetherRoom(
                roomName,
                book,
                true,
                1234,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(5),
                5
        ));

        UserRoom userRoom = UserRoom.builder()
                .userRole(UserRole.MEMBER)
                .userPercentage(0.0)
                .user(user)
                .currentPage(1)
                .room(room)
                .build();

        userRoomRepository.save(userRoom);
        entityManager.flush();
        entityManager.clear();

        // When
        UserRoom foundUserRoom = userRoomRepository.findById(userRoom.getUserRoomId()).orElseThrow();
        foundUserRoom.updateUserProgress(50.0, 100);

        userRoomRepository.save(foundUserRoom);
        entityManager.flush();
        entityManager.clear();

        // Then
        UserRoom updatedUserRoom = userRoomRepository.findById(foundUserRoom.getUserRoomId()).orElseThrow();
        assertThat(updatedUserRoom.getUserPercentage()).isEqualTo(50.0);
        assertThat(updatedUserRoom.getCurrentPage()).isEqualTo(100);
    }
}
