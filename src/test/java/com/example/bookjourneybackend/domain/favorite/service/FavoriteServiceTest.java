package com.example.bookjourneybackend.domain.favorite.service;

import com.example.bookjourneybackend.domain.book.domain.Book;
import com.example.bookjourneybackend.domain.book.domain.repository.BookRepository;
import com.example.bookjourneybackend.domain.book.dto.response.GetBookInfoResponse;
import com.example.bookjourneybackend.domain.book.service.BookCacheService;
import com.example.bookjourneybackend.domain.book.service.BookService;
import com.example.bookjourneybackend.domain.favorite.domain.Favorite;
import com.example.bookjourneybackend.domain.favorite.domain.dto.request.DeleteFavoriteSelectedRequest;
import com.example.bookjourneybackend.domain.favorite.domain.dto.response.GetFavoriteListResponse;
import com.example.bookjourneybackend.domain.favorite.domain.dto.response.PostFavoriteAddResponse;
import com.example.bookjourneybackend.domain.favorite.domain.repository.FavoriteRepository;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.domain.user.domain.repository.UserRepository;
import com.example.bookjourneybackend.global.util.DateUtil;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookCacheService bookCacheService;

    @Mock
    private DateUtil dateUtil;

    @Mock
    private BookService bookService;

    @PersistenceContext
    private final EntityManager entityManager = mock(EntityManager.class);

    @InjectMocks
    private FavoriteService favoriteService;

    private User mockUser;
    private Book mockBook;
    private Favorite mockFavorite;

    private final Long userId = 1L;
    private final String isbn = "1234567890";

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(userId)
                .email("test@example.com")
                .password("password")
                .nickname("nickname")
                .build();

        mockBook = Book.builder()
                .isbn(isbn)
                .bookTitle("Test Book")
                .authorName("Test Author")
                .publisher("Test Publisher")
                .build();

        mockFavorite = Favorite.builder()
                .user(mockUser)
                .book(mockBook)
                .build();
    }

    @Test
    @DisplayName("즐겨찾기 추가")
    void addFavorite() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(favoriteRepository.existsFavoriteByUserIdAndIsbn(userId, isbn)).thenReturn(false);
        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.empty());
        when(bookCacheService.checkBookInfo(isbn)).thenReturn(new GetBookInfoResponse(isbn, "Test Book", "Test Author", "Test Publisher", "Genre", "2023-01-01", "Description", "imageUrl"));

        // when
        PostFavoriteAddResponse response = favoriteService.addFavorite(isbn, userId);

        // then
        assertNotNull(response);
        assertTrue(response.isFavorite());
        verify(favoriteRepository).save(any(Favorite.class));
    }

    @Test
    @DisplayName("즐겨찾기 목록 조회")
    void showFavoriteList() {
        // given
        List<Favorite> favoriteList = List.of(mockFavorite);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(favoriteRepository.findByUserOrderByCreatedAtDesc(mockUser)).thenReturn(Optional.of(favoriteList));

        // when
        GetFavoriteListResponse response = favoriteService.showFavoriteList(userId);

        // then
        assertNotNull(response);
        assertEquals(1, response.getFavoriteList().size());
        assertEquals(mockBook.getBookTitle(), response.getFavoriteList().get(0).getBookInfo().getBookTitle());
    }


    @Test
    @DisplayName("isbn으로 즐겨찾기 삭제")
    void deleteFavorite() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(favoriteRepository.existsFavoriteByUserIdAndIsbn(userId, isbn)).thenReturn(true);
        when(bookRepository.findByIsbn(isbn)).thenReturn(Optional.of(mockBook));

        // when
        PostFavoriteAddResponse response = favoriteService.deleteFavorite(isbn, userId);

        // then
        assertNotNull(response);
        assertFalse(response.isFavorite());
        verify(favoriteRepository).deleteFavoriteByUserAndBook(mockUser, mockBook);
        verify(bookService).deleteBook(mockBook);
    }
}
