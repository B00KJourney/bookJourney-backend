package com.example.bookjourneybackend.domain.user.domain.repository;

import com.example.bookjourneybackend.domain.book.domain.Book;
import com.example.bookjourneybackend.domain.book.domain.repository.BookRepository;
import com.example.bookjourneybackend.domain.favorite.domain.Favorite;
import com.example.bookjourneybackend.domain.favorite.domain.repository.FavoriteRepository;
import com.example.bookjourneybackend.domain.room.domain.Room;
import com.example.bookjourneybackend.domain.room.domain.RoomType;
import com.example.bookjourneybackend.domain.room.domain.repository.RoomRepository;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRoom;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRole;
import com.example.bookjourneybackend.domain.userRoom.domain.repository.UserRoomRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.example.bookjourneybackend.domain.book.domain.GenreType.NOVEL_POETRY_DRAMA;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRoomRepository userRoomRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Test
    @DisplayName("User 엔티티 저장 및 조회 테스트")
    void saveAndFindUser() {
        // given
        User user = User.builder()
                .email("test@example.com")
                .password("verysecret123")
                .nickname("testUser")
                .imageUrl("profile.jpg")
                .build();

        // when
        User savedUser = userRepository.save(user);
        Optional<User> foundUser = userRepository.findById(savedUser.getUserId());

        // then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getEmail()).isEqualTo("test@example.com");
        assertThat(foundUser.get().getNickname()).isEqualTo("testUser");
    }

    @Test
    @DisplayName("User와 Book 간의 Favorite 연관관계 테스트")
    void saveUserWithFavorite() {
        // given
        User user = userRepository.save(User.builder()
                .email("favorite@example.com")
                .password("verysecret123")
                .nickname("favoriteUser")
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

        Favorite favorite = Favorite.builder()
                .user(user)
                .book(book)
                .build();

        // when
        Favorite savedFavorite = favoriteRepository.save(favorite);

        // then
        assertThat(savedFavorite.getUser()).isEqualTo(user);
        assertThat(savedFavorite.getBook()).isEqualTo(book);
        assertThat(favoriteRepository.findAll()).hasSize(1);
    }

    @Test
    @DisplayName("User와 Room 간의 UserRoom 연관관계 테스트")
    void saveUserWithUserRoom() {
        // given
        User user = userRepository.save(User.builder()
                .email("room@example.com")
                .password("verysecret123")
                .nickname("roomUser")
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

        Room room = roomRepository.save(Room.builder()
                .roomType(RoomType.TOGETHER)
                .roomName("테스트 방")
                .book(book)
                .isPublic(true)
                .roomPercentage(0.0)
                .startDate(LocalDate.now())
                .progressEndDate(LocalDate.now().plusDays(30))
                .recruitCount(5)
                .build());

        UserRoom userRoom = UserRoom.builder()
                .user(user)
                .room(room)
                .userRole(UserRole.MEMBER)
                .userPercentage(50.0)
                .currentPage(150)
                .build();

        // when
        UserRoom savedUserRoom = userRoomRepository.save(userRoom);

        // then
        assertThat(savedUserRoom.getUser()).isEqualTo(user);
        assertThat(savedUserRoom.getRoom()).isEqualTo(room);
        assertThat(userRoomRepository.findAll()).hasSize(1);
    }

}
