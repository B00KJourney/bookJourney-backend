package com.example.bookjourneybackend.domain.room.service;

import com.example.bookjourneybackend.domain.book.domain.Book;
import com.example.bookjourneybackend.domain.room.domain.Room;
import com.example.bookjourneybackend.domain.room.domain.RoomType;
import com.example.bookjourneybackend.domain.room.dto.response.GetRoomArchiveResponse;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.domain.user.domain.repository.UserRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.example.bookjourneybackend.global.entity.EntityStatus.*;
import static com.example.bookjourneybackend.global.response.status.BaseExceptionResponseStatus.CANNOT_FOUND_USER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoomArchiveServiceTest {

    @InjectMocks
    private RoomArchiveService roomArchiveService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserRoomRepository userRoomRepository;

    @Mock
    private DateUtil dateUtil;

    private User testUser;
    private Room testRoom;
    private Book testBook;
    private UserRoom testUserRoomInactive;
    private UserRoom testUserRoomExpired;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(1L)
                .email("test@example.com")
                .password("test1234")
                .nickname("testUser")
                .imageUrl("testImageUrl.jpg")
                .build();

        testBook = Book.builder()
                .bookId(1L)
                .isbn("1234567890")
                .bookTitle("테스트 책")
                .authorName("테스트 저자")
                .pageCount(300)
                .imageUrl("testImageUrl.jpg")
                .bestSeller(false)
                .build();

        testRoom = Room.builder()
                .roomId(1L)
                .roomType(RoomType.TOGETHER)
                .roomName("테스트 방")
                .book(testBook)
                .isPublic(true)
                .roomPercentage(0.0)
                .startDate(LocalDate.of(2024, 1, 1))
                .progressEndDate(LocalDate.of(2024, 2, 1))
                .recruitCount(5)
                .build();

        testUserRoomInactive = UserRoom.builder()
                .userRole(com.example.bookjourneybackend.domain.userRoom.domain.UserRole.MEMBER)
                .userPercentage(50.0)
                .user(testUser)
                .currentPage(150)
                .room(testRoom)
                .inActivatedAt(LocalDateTime.now().minusDays(5)) // 최근 비활성화된 방
                .build();

        testUserRoomExpired = UserRoom.builder()
                .userRole(com.example.bookjourneybackend.domain.userRoom.domain.UserRole.MEMBER)
                .userPercentage(100.0)
                .user(testUser)
                .currentPage(300)
                .room(testRoom)
                .inActivatedAt(LocalDateTime.now().minusDays(30)) // 오래 전에 만료된 방
                .build();
    }

    @Test
    @DisplayName("독서기록장 - 다 안읽었어요 조회(INACTIVE 상태) 성공")
    void viewArchiveRoomsInactiveSuccess() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRoomRepository.findInActiveTogetherRoomsByUserIdAndDate(1L, 2024, 1, INACTIVE))
                .thenReturn(List.of(testUserRoomInactive));
        when(userRoomRepository.findInActiveAloneRoomsByUserIdAndDate(1L, 2024, 1, INACTIVE))
                .thenReturn(List.of());
        when(dateUtil.extractDateFromLocalDateTime(any())).thenReturn("2024-01-01");

        // when
        GetRoomArchiveResponse response = roomArchiveService.viewArchiveRooms(1L, 1, 2024, INACTIVE);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRecordList()).hasSize(1);
        assertThat(response.getRecordList().get(0).getRoomId()).isEqualTo(1L);

        verify(userRoomRepository).findInActiveTogetherRoomsByUserIdAndDate(1L, 2024, 1, INACTIVE);
        verify(userRoomRepository).findInActiveAloneRoomsByUserIdAndDate(1L, 2024, 1, INACTIVE);
    }

    @Test
    @DisplayName("독서기록장 - 다 읽었어요 조회(EXPIRED 상태) 성공")
    void viewArchiveRoomsExpiredSuccess() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRoomRepository.findInActiveTogetherRoomsByUserIdAndDate(1L, 2024, 1, EXPIRED))
                .thenReturn(List.of(testUserRoomExpired));
        when(userRoomRepository.findInActiveAloneRoomsByUserIdAndDate(1L, 2024, 1, EXPIRED))
                .thenReturn(List.of());
        when(dateUtil.extractDateFromLocalDateTime(any())).thenReturn("2024-01-01");

        // when
        GetRoomArchiveResponse response = roomArchiveService.viewArchiveRooms(1L, 1, 2024, EXPIRED);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRecordList()).hasSize(1);
        assertThat(response.getRecordList().get(0).getRoomId()).isEqualTo(1L);

        verify(userRoomRepository).findInActiveTogetherRoomsByUserIdAndDate(1L, 2024, 1, EXPIRED);
        verify(userRoomRepository).findInActiveAloneRoomsByUserIdAndDate(1L, 2024, 1, EXPIRED);
    }

    @Test
    @DisplayName("독서기록장 조회 - 유저가 존재하지 않으면 예외 발생")
    void viewArchiveRoomsUserNotFoundThrowsException() {
        // given
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        GlobalException exception = assertThrows(GlobalException.class, () ->
                roomArchiveService.viewArchiveRooms(1L, 1, 2024, INACTIVE));

        assertThat(exception.getExceptionStatus()).isEqualTo(CANNOT_FOUND_USER);
    }
}
