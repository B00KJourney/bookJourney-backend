package com.example.bookjourneybackend.domain.room.service;

import com.example.bookjourneybackend.domain.book.domain.Book;
import com.example.bookjourneybackend.domain.book.domain.GenreType;
import com.example.bookjourneybackend.domain.book.domain.repository.BookRepository;
import com.example.bookjourneybackend.domain.favorite.domain.repository.FavoriteRepository;
import com.example.bookjourneybackend.domain.recentSearch.service.RecentSearchService;
import com.example.bookjourneybackend.domain.record.domain.repository.RecordRepository;
import com.example.bookjourneybackend.domain.room.domain.Room;
import com.example.bookjourneybackend.domain.room.domain.RoomType;
import com.example.bookjourneybackend.domain.room.domain.repository.RoomRepository;
import com.example.bookjourneybackend.domain.room.dto.request.PostRoomCreateRequest;
import com.example.bookjourneybackend.domain.room.dto.response.GetRoomDetailResponse;
import com.example.bookjourneybackend.domain.room.dto.response.PostRoomCreateResponse;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.domain.user.domain.repository.UserRepository;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRole;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRoom;
import com.example.bookjourneybackend.domain.userRoom.domain.repository.UserRoomRepository;
import com.example.bookjourneybackend.global.exception.GlobalException;
import com.example.bookjourneybackend.global.util.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.example.bookjourneybackend.global.response.status.BaseExceptionResponseStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @InjectMocks
    private RoomService roomService;

    @Mock
    private RecentSearchService recentSearchService;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoomRepository userRoomRepository;

    @Mock
    private DateUtil dateUtil;

    @Mock
    private RecordRepository recordRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    private User testUser;
    private Room testRoom;
    private Book testBook;
    private UserRoom testUserRoom;

    @BeforeEach
    void beforeEach() {
        testUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .password("test1234")
                .nickname("testUser")
                .imageUrl("testImageUrl")
                .build();

        testBook = Book.builder()
                .bookId(1L)
                .isbn("1234567890")
                .bookTitle("테스트 책")
                .authorName("테스트 저자")
                .genre(GenreType.NOVEL_POETRY_DRAMA)
                .pageCount(300)
                .imageUrl("testImageUrl")
                .bestSeller(false)
                .build();

        testRoom = Room.builder()
                .roomId(1L)
                .roomType(RoomType.TOGETHER)
                .roomName("테스트 방")
                .book(testBook)
                .isPublic(true)
                .password(null)
                .roomPercentage(0.0)
                .startDate(LocalDate.now())
                .progressEndDate(LocalDate.now().plusDays(7))
                .recruitEndDate(LocalDate.now().plusDays(5))
                .recruitCount(5)
                .build();

        testUserRoom = UserRoom.builder()
                .userRole(UserRole.MEMBER)
                .userPercentage(0.0)
                .user(testUser)
                .currentPage(0)
                .room(testRoom)
                .build();

        testBook.addRoom(testRoom);
        testRoom.addUserRoom(testUserRoom);
    }


    @Test
    @DisplayName("방 상세 정보 조회 - 성공")
    void showRoomDetailsSuccess() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.of(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(favoriteRepository.existsActiveFavoriteByUserIdAndBook(1L, testBook)).thenReturn(true);
        when(userRoomRepository.existsByRoomAndUser(testRoom, testUser)).thenReturn(true);

        // when
        GetRoomDetailResponse response = roomService.showRoomDetails(1L, 1L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRoomName()).isEqualTo("테스트 방");
        assertThat(response.isMember()).isTrue();
        assertThat(response.isFavorite()).isTrue();

        verify(roomRepository).findById(1L);
        verify(userRepository).findById(1L);
    }

    @Test
    @DisplayName("방 상세 정보 조회 - 방이 존재하지 않으면 예외 발생")
    void showRoomDetailsRoomNotFoundThrowsException() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        GlobalException exception = assertThrows(GlobalException.class, () -> roomService.showRoomDetails(1L, 1L));
        assertThat(exception.getExceptionStatus()).isEqualTo(CANNOT_FOUND_ROOM);

        verify(roomRepository).findById(1L);
        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("방 생성 - 성공")
    void createRoomSuccess() {
        // given
        PostRoomCreateRequest request = new PostRoomCreateRequest(
                true,
                "테스트 방",
                "2024-03-01",
                "2024-04-01",
                5,
                null,
                "1234567890"
        );

        when(bookRepository.findByIsbn("1234567890")).thenReturn(Optional.of(testBook));
        when(userRepository.findByUserIdAndStatus(eq(1L), any())).thenReturn(Optional.of(testUser));
        when(roomRepository.save(any(Room.class))).thenReturn(testRoom);

        // when
        PostRoomCreateResponse response = roomService.createRoom(request, 1L);

        // then
        assertThat(response).isNotNull();   //Mock 기반 테스트이므로 실제 DB에 저장이 안되므로 도메인 테스트에서 실제 DB 저장 확인

        verify(bookRepository).findByIsbn("1234567890");
        verify(roomRepository).save(any(Room.class));
    }


    @Test
    @DisplayName("방 검색 - 성공")
    void searchRoomsSuccess() {
        // given
        Slice<Room> roomSlice = new SliceImpl<>(List.of(testRoom));
        when(roomRepository.findRoomsByFilters(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(roomSlice);
        when(dateUtil.parseDate(any())).thenReturn(LocalDate.now());
//        when(recentSearchService.addRecentSearch(any(), "테스트")).

        // when
        var bookTitleResponse = roomService.searchRooms("테스트", "책 제목", null, null, null, null, null, 10, 0, 1L);
        var roomNameResponse = roomService.searchRooms("테스트", "방 이름", null, null, null, null, null, 10, 0, 1L);
        var authorNameResponse = roomService.searchRooms("테스트", "작가 이름", null, null, null, null, null, 10, 0, 1L);

        // then
        assertThat(bookTitleResponse).isNotNull();
        assertThat(bookTitleResponse.getRoomList()).hasSize(1);
        assertThat(bookTitleResponse.getRoomList().get(0).getBookTitle()).isEqualTo("테스트 책");

        assertThat(roomNameResponse).isNotNull();
        assertThat(roomNameResponse.getRoomList()).hasSize(1);
        assertThat(roomNameResponse.getRoomList().get(0).getRoomName()).isEqualTo("테스트 방");

        assertThat(authorNameResponse).isNotNull();
        assertThat(authorNameResponse.getRoomList()).hasSize(1);
        assertThat(authorNameResponse.getRoomList().get(0).getAuthorName()).isEqualTo("테스트 저자");

        verify(roomRepository, times(3)).findRoomsByFilters(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("방 검색 - 검색어가 없을 경우 예외 발생")
    void searchRoomsEmptySearchTermThrowsException() {
        // when & then
        GlobalException exception = assertThrows(GlobalException.class, () -> roomService.searchRooms("", "TITLE", null, null, null, null, null, 10, 0, 1L));
        assertThat(exception.getExceptionStatus()).isEqualTo(EMPTY_SEARCH_TERM);
    }

    @Test
    @DisplayName("방 탈퇴 - 성공 (멤버)")
    void exitRoomSuccess() {
        // given
        when(roomRepository.findById(1L)).thenReturn(Optional.ofNullable(testRoom));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRoomRepository.findUserRoomByRoomAndUserAndStatus(any(), any(), any()))
                .thenReturn(Optional.of(testUserRoom));

        // when
        roomService.exitRoom(1L, 1L);

        // then
        verify(userRoomRepository).delete(any(UserRoom.class));
    }
}
