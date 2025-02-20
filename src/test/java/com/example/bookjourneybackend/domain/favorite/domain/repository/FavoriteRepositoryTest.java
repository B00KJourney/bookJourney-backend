package com.example.bookjourneybackend.domain.favorite.domain.repository;

import com.example.bookjourneybackend.domain.book.domain.Book;

import com.example.bookjourneybackend.domain.book.domain.repository.BookRepository;
import com.example.bookjourneybackend.domain.favorite.domain.Favorite;
import com.example.bookjourneybackend.domain.user.domain.User;

import com.example.bookjourneybackend.domain.user.domain.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.example.bookjourneybackend.domain.book.domain.GenreType.NOVEL_POETRY_DRAMA;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
class FavoriteRepositoryTest {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BookRepository bookRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    @DisplayName("Favorite 저장 및 연관관계 테스트")
    void saveFavoriteTest() {
        String email = "user@example.com";
        String bookTitle = "테스트 책";

        // given
        User user = userRepository.save(User.builder()
                .email(email)
                .password("verysecret123")
                .nickname("user123")
                .imageUrl("profile.jpg")
                .build());
        Book book = bookRepository.save(Book.builder()
                .bookTitle(bookTitle)
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
        entityManager.flush();
        entityManager.clear();

        // then
        Favorite foundFavorite = favoriteRepository.findById(savedFavorite.getFavoriteId()).orElseThrow();
        assertThat(foundFavorite).isNotNull();
        assertThat(foundFavorite.getUser().getEmail()).isEqualTo(email);
        assertThat(foundFavorite.getBook().getBookTitle()).isEqualTo(bookTitle);
    }
}
