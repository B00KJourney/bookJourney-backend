package com.example.bookjourneybackend.domain.room.domain.repository;

import com.example.bookjourneybackend.domain.book.domain.Book;

import com.example.bookjourneybackend.domain.book.domain.repository.BookRepository;
import com.example.bookjourneybackend.domain.record.domain.Record;
import com.example.bookjourneybackend.domain.record.domain.RecordType;
import com.example.bookjourneybackend.domain.record.domain.repository.RecordRepository;
import com.example.bookjourneybackend.domain.room.domain.Room;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.domain.user.domain.repository.UserRepository;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRole;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRoom;
import com.example.bookjourneybackend.domain.userRoom.domain.repository.UserRoomRepository;
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
class RoomRepositoryTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoomRepository userRoomRepository;

    @Autowired
    private RecordRepository recordRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("Room 엔티티 저장 및 조회 테스트")
    void saveRoomTest() {
        String bookTitle = "테스트 책";

        // given
        Book book = bookRepository.save(Book.builder()
                .bookTitle(bookTitle)
                .isbn("1234567890123")
                .authorName("테스트 저자")
                .genre(NOVEL_POETRY_DRAMA)
                .imageUrl("test.jpg")
                .bestSeller(false)
                .build());

        Room room = Room.makeReadTogetherRoom(
                "독서 모임",
                book,
                true,
                1234,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                LocalDate.now().plusDays(5),
                5
        );

        // when
        Room savedRoom = roomRepository.save(room);
        entityManager.flush();
        entityManager.clear();

        // then
        Room foundRoom = roomRepository.findById(savedRoom.getRoomId()).orElseThrow();
        assertThat(foundRoom).isNotNull();
        assertThat(foundRoom.getRoomName()).isEqualTo("독서 모임");
        assertThat(bookRepository.findById(foundRoom.getBook().getBookId()).get().getBookTitle()).isEqualTo(bookTitle);
    }

    @Test
    @DisplayName("Room과 UserRoom의 연관관계 테스트")
    void roomUserRoomRelationTest() {
        String email = "user@example.com";

        // given
        Book book = bookRepository.save(Book.builder()
                .bookTitle("테스트 책")
                .isbn("1234567890123")
                .authorName("테스트 저자")
                .genre(NOVEL_POETRY_DRAMA)
                .imageUrl("test.jpg")
                .bestSeller(false)
                .build());

        Room room = roomRepository.save(Room.makeReadAloneRoom(book));

        User user = userRepository.save(User.builder()
                .email(email)
                .password("verysecret123")
                .nickname("user123")
                .imageUrl("profile.jpg")
                .build());

        UserRoom userRoom = new UserRoom(UserRole.MEMBER, 0.0, user, 1, room, LocalDateTime.now());
        userRoomRepository.save(userRoom);

        entityManager.flush();
        entityManager.clear();

        // when
        Room foundRoom = roomRepository.findById(room.getRoomId()).orElseThrow();

        // then
        assertThat(foundRoom.getUserRooms()).hasSize(1);
        assertThat(foundRoom.getUserRooms().get(0).getUser().getEmail()).isEqualTo(email);
    }

    @Test
    @DisplayName("Room과 Record의 연관관계 테스트")
    void roomRecordRelationTest() {
        // given
        Book book = bookRepository.save(Book.builder()
                .bookTitle("테스트 책")
                .isbn("1234567890123")
                .authorName("테스트 저자")
                .genre(NOVEL_POETRY_DRAMA)
                .imageUrl("test.jpg")
                .bestSeller(false)
                .build());

        Room room = roomRepository.save(Room.makeReadAloneRoom(book));

        User user = userRepository.save(User.builder()
                .email("favorite@example.com")
                .password("verysecret123")
                .nickname("user123")
                .imageUrl("profile.jpg")
                .build());

        String recordTitle = "독서 기록";

        Record record = Record.builder()
                .room(room)
                .user(user)
                .recordTitle(recordTitle)
                .recordType(RecordType.ENTIRE)
//                .recordPage(50)
                .content("이 책 정말 재미있어요!")
                .build();

        recordRepository.save(record);

        entityManager.flush();
        entityManager.clear();

        // when
        Room foundRoom = roomRepository.findById(room.getRoomId()).orElseThrow();

        // then
        assertThat(foundRoom.getRecords()).hasSize(1);
        assertThat(foundRoom.getRecords().get(0).getRecordTitle()).isEqualTo(recordTitle);
    }
}
