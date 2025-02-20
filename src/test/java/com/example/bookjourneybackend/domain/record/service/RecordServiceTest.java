package com.example.bookjourneybackend.domain.record.service;

import com.example.bookjourneybackend.domain.book.domain.Book;
import com.example.bookjourneybackend.domain.book.domain.repository.BookRepository;
import com.example.bookjourneybackend.domain.record.domain.Record;
import com.example.bookjourneybackend.domain.record.domain.repository.RecordLikeRepository;
import com.example.bookjourneybackend.domain.record.domain.repository.RecordRepository;
import com.example.bookjourneybackend.domain.record.dto.request.PostRecordRequest;
import com.example.bookjourneybackend.domain.record.dto.response.GetRecordResponse;
import com.example.bookjourneybackend.domain.record.dto.response.PostRecordLikeResponse;
import com.example.bookjourneybackend.domain.record.dto.response.PostRecordPageResponse;
import com.example.bookjourneybackend.domain.record.dto.response.PostRecordResponse;
import com.example.bookjourneybackend.domain.room.domain.Room;
import com.example.bookjourneybackend.domain.room.domain.repository.RoomRepository;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.domain.user.domain.repository.UserRepository;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRoom;
import com.example.bookjourneybackend.domain.userRoom.domain.repository.UserRoomRepository;
import com.example.bookjourneybackend.global.exception.GlobalException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static com.example.bookjourneybackend.domain.book.domain.GenreType.NOVEL_POETRY_DRAMA;
import static com.example.bookjourneybackend.domain.record.domain.RecordType.ENTIRE;
import static com.example.bookjourneybackend.domain.record.domain.RecordType.PAGE;
import static com.example.bookjourneybackend.domain.room.domain.RoomType.TOGETHER;
import static com.example.bookjourneybackend.domain.userRoom.domain.UserRole.HOST;
import static com.example.bookjourneybackend.global.entity.EntityStatus.*;
import static com.example.bookjourneybackend.global.response.status.BaseExceptionResponseStatus.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class RecordServiceTest {

    @Autowired
    private RecordService recordService;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserRoomRepository userRoomRepository;

    @Autowired
    RecordLikeRepository recordLikeRepository;

    @Autowired
    private EntityManager em;

    private User mockUser;
    private Book mockBook;
    private Room mockRoom;
    private UserRoom mockUserRoom;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .email("test@test.com")
                .nickname("test")
                .password("test1234")
                .imageUrl("test.jpg")
                .build();
        userRepository.save(mockUser);

        mockBook = Book.builder()
                .bookTitle("테스트 책")
                .authorName("테스트 작가")
                .isbn("123456789")
                .genre(NOVEL_POETRY_DRAMA)
                .pageCount(300)
                .imageUrl("test.jpg")
                .build();
        bookRepository.save(mockBook);

        mockRoom = Room.builder()
                .roomName("테스트 방")
                .isPublic(true)
                .recruitCount(5)
                .roomType(TOGETHER)
                .roomPercentage(0.0)
                .book(mockBook)
                .build();
        roomRepository.save(mockRoom);

        mockUserRoom = UserRoom.builder()
                .room(mockRoom)
                .user(mockUser)
                .userPercentage(0.0)
                .userRole(HOST)
                .currentPage(0)
                .build();
        userRoomRepository.save(mockUserRoom);

        em.flush();
        em.clear();
    }

    @AfterEach
    void tearDown() {
        recordRepository.deleteAll();
        userRoomRepository.deleteAll();
        roomRepository.deleteAll();
        bookRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("기록이 정상적으로 생성되는 경우")
    void createRecordSuccess() {
        // given
        PostRecordRequest request = new PostRecordRequest("페이지 기록", null, 50, "기록 내용");

        // when
        PostRecordResponse response = recordService.createRecord(request, mockRoom.getRoomId(), mockUser.getUserId());

        // then
        assertNotNull(response);
        assertThat(response.getRecordId()).isNotNull();
        assertThat(recordRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("방이 EXPIRED 상태라서 기록을 생성할 수 없는 경우")
    void createRecordFailRoomExpired() {
        // given
        mockRoom.setStatus(EXPIRED);
        roomRepository.save(mockRoom);
        PostRecordRequest request = new PostRecordRequest("페이지 기록", null, 50, "기록 내용");

        // when & then
        GlobalException exception = assertThrows(GlobalException.class, () -> recordService.createRecord(request, mockRoom.getRoomId(), mockUser.getUserId()));
        assertEquals(CANNOT_WRITE_IN_EXPIRED_ROOM, exception.getExceptionStatus());
    }


    @Test
    @DisplayName("입력한 페이지가 책 페이지 수보다 커서 오류가 발생하는 경우")
    void createRecordFailInvalidPage() {
        // given
        int invalidPage = mockBook.getPageCount() + 1;  // 책의 총 페이지 수보다 큰 값 설정
        PostRecordRequest request = new PostRecordRequest("페이지 기록", null, invalidPage, "기록 내용");

        // when & then
        GlobalException exception = assertThrows(GlobalException.class, () -> recordService.createRecord(request, mockRoom.getRoomId(), mockUser.getUserId()));
        assertEquals(INVALID_PAGE_NUMBER, exception.getExceptionStatus());
    }

    @Test
    @DisplayName("UserRoom이 INACTIVE 상태에서 기록이 생성되는 경우")
    void createRecordSuccessUserRoomInactive() {
        // given
        mockUserRoom.setStatus(INACTIVE);
        userRoomRepository.save(mockUserRoom);

        PostRecordRequest request = new PostRecordRequest("페이지 기록", null, 30, "기록 내용");

        // when
        PostRecordResponse response = recordService.createRecord(request, mockRoom.getRoomId(), mockUser.getUserId());

        // then
        assertNotNull(response);
        assertThat(response.getRecordId()).isNotNull();
        assertThat(recordRepository.count()).isEqualTo(1);

        // UserRoom 상태가 ACTIVE로 변경되었는지 확인
        UserRoom updatedUserRoom = userRoomRepository.findUserRoomByRoomAndUser(mockRoom, mockUser).get();
        assertThat(updatedUserRoom.getStatus()).isEqualTo(ACTIVE);
    }

    @Test
    @DisplayName("특정 방의 전체 기록을 정상적으로 조회하는 경우")
    void showEntireRecordsSuccess() {
        // given
        Record entireRecord = Record.builder()
                .room(mockRoom)
                .user(mockUser)
                .recordType(ENTIRE)
                .recordTitle("전체 기록 테스트")
                .content("이것은 전체 기록 테스트입니다.")
                .build();
        recordRepository.save(entireRecord);

        // when
        GetRecordResponse response = recordService.showEntireRecords(mockRoom.getRoomId(), mockUser.getUserId(), "최신 등록순");

        // then
        assertThat(response.getRecordList()).isNotNull();  // 리스트가 null이 아닌지 확인
        assertFalse(response.getRecordList().isEmpty());   // 리스트가 비어있지 않은지 확인
        assertThat(response.getRecordList().size()).isEqualTo(1);
        assertThat(response.getRecordList().get(0).getRecordTitle()).isEqualTo(entireRecord.getRecordTitle());
    }

    @Test
    @DisplayName("특정 페이지 범위 내의 기록을 조회하는 경우")
    void showPageRecordsSuccess() {
        // given
        Record pageRecord = Record.builder()
                .room(mockRoom)
                .user(mockUser)
                .recordType(PAGE)
                .recordPage(150)
                .content("이것은 페이지 기록 테스트입니다.")
                .build();
        recordRepository.save(pageRecord);

        // when
        GetRecordResponse response = recordService.showPageRecords(mockRoom.getRoomId(), mockUser.getUserId(), "페이지순", 100, 200);

        // then
        assertThat(response.getRecordList()).isNotNull();  // 리스트가 null이 아닌지 확인
        assertFalse(response.getRecordList().isEmpty());   // 리스트가 비어있지 않은지 확인
        assertThat(response.getRecordList().size()).isEqualTo(1);
        assertThat(response.getRecordList().get(0).getContent()).isEqualTo(pageRecord.getContent());
    }


    @Test
    @DisplayName("페이지 범위를 벗어난 경우 빈 리스트 반환")
    void showPageRecordsFailInvalidPageRange() {
        // given
        Record pageRecord = Record.builder()
                .room(mockRoom)
                .user(mockUser)
                .recordType(PAGE)
                .recordPage(50)
                .content("이것은 페이지 기록 테스트입니다.")
                .build();
        recordRepository.save(pageRecord);

        // when
        GetRecordResponse response = recordService.showPageRecords(mockRoom.getRoomId(), mockUser.getUserId(), "페이지순", 200, 300);

        // then
        assertThat(response.getRecordList()).isNotNull();
        assertThat(response.getRecordList().size()).isEqualTo(0);
    }

    @Test
    @DisplayName("현재 읽은 페이지 기록하기")
    void enterRecordPageSuccess() {
        // when
        PostRecordPageResponse response = recordService.enterRecordPage(mockRoom.getRoomId(), mockUser.getUserId(), 150);

        // then
        UserRoom updatedUserRoom = userRoomRepository.findById(mockUserRoom.getUserRoomId()).orElseThrow();  // 업데이트 된 데이터 새로 조회
        assertNotNull(response);
        assertThat(response.getCurrentPage()).isEqualTo(150);
        assertThat(updatedUserRoom.getCurrentPage()).isEqualTo(150);
    }

    @Test
    @DisplayName("현재 읽은 페이지가 책 페이지 수를 초과하는 경우")
    void enterRecordPageFailInvalidPage() {
        // when & then: 예외 발생 검증
        GlobalException exception = assertThrows(GlobalException.class,
                () -> recordService.enterRecordPage(mockRoom.getRoomId(), mockUser.getUserId(), 400));
        assertEquals(INVALID_PAGE_NUMBER, exception.getExceptionStatus());
    }

    @Test
    @DisplayName("방이 EXPIRED 상태라서 페이지 기록을 남길 수 없는 경우")
    void enterRecordPageFailRoomExpired() {
        // given
        mockRoom.setStatus(EXPIRED);
        roomRepository.save(mockRoom);

        // when & then
        GlobalException exception = assertThrows(GlobalException.class, () -> recordService.enterRecordPage(mockRoom.getRoomId(), mockUser.getUserId(), 100));
        assertEquals(CANNOT_ENTER_PAGE_IN_EXPIRED_ROOM, exception.getExceptionStatus());
    }

    @Test
    @DisplayName("기록 좋아요를 누르는 경우")
    void toggleRecordLikeSuccess() {
        // given
        Record record = Record.builder()
                .room(mockRoom)
                .user(mockUser)
                .recordType(PAGE)
                .recordPage(150)
                .content("기록")
                .build();
        recordRepository.save(record);
        em.flush();
        em.clear();

        // when
        PostRecordLikeResponse response = recordService.toggleRecordLike(record.getRecordId(), mockUser.getUserId());

        // then
        assertTrue(response.isLiked());
        assertThat(recordLikeRepository.count()).isEqualTo(1);
    }


    @Test
    @DisplayName("기록 좋아요를 취소하는 경우")
    void toggleRecordLikeCancel() {
        // given
        Record record = Record.builder()
                .room(mockRoom)
                .user(mockUser)
                .recordType(PAGE)
                .recordPage(150)
                .content("기록")
                .build();
        recordRepository.save(record);

        recordService.toggleRecordLike(record.getRecordId(), mockUser.getUserId());
        assertThat(recordLikeRepository.count()).isEqualTo(1);

        // when
        PostRecordLikeResponse response = recordService.toggleRecordLike(record.getRecordId(), mockUser.getUserId());

        // then
        assertFalse(response.isLiked());
        assertThat(recordLikeRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("방에 속해있지 않은 경우 좋아요 불가")
    void toggleRecordLikeFailNotInRoom() {
        // given
        User otherUser = User.builder()
                .email("other@test.com")
                .nickname("otherUser")
                .password("password")
                .imageUrl("test.jpg")
                .build();
        userRepository.save(otherUser);

        Record record = Record.builder()
                .room(mockRoom)
                .user(mockUser)
                .recordType(PAGE)
                .recordPage(150)
                .content("기록")
                .build();
        recordRepository.save(record);

        // when & then
        GlobalException exception = assertThrows(GlobalException.class,
                () -> recordService.toggleRecordLike(record.getRecordId(), otherUser.getUserId()));
        assertEquals(NOT_PARTICIPATING_IN_ROOM, exception.getExceptionStatus());
    }

    @Test
    @DisplayName("방이 EXPIRED 상태이면 좋아요 토글 불가")
    void toggleRecordLikeFail_ExpiredRoom() {
        // given
        mockRoom.setStatus(EXPIRED);
        roomRepository.save(mockRoom);

        Record record = Record.builder()
                .room(mockRoom)
                .user(mockUser)
                .recordType(PAGE)
                .recordPage(150)
                .content("기록")
                .build();
        recordRepository.save(record);

        // when & then
        GlobalException exception = assertThrows(GlobalException.class,
                () -> recordService.toggleRecordLike(record.getRecordId(), mockUser.getUserId()));
        assertEquals(CANNOT_LIKE_IN_EXPIRED_ROOM, exception.getExceptionStatus());
    }

    @Test
    @DisplayName("기록을 정상적으로 삭제하는 경우")
    void deleteRecordSuccess() {
        // given
        Record record = Record.builder()
                .room(mockRoom)
                .user(mockUser)
                .recordType(PAGE)
                .recordPage(150)
                .content("기록")
                .build();
        recordRepository.save(record);
        assertThat(recordRepository.count()).isEqualTo(1);

        // when
        recordService.deleteRecord(record.getRecordId(), mockUser.getUserId());

        // then
        assertThat(recordRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("방이 EXPIRED 상태이면 기록 삭제 불가")
    void deleteRecordFailExpiredRoom() {
        // given
        mockUserRoom.setStatus(EXPIRED);
        userRoomRepository.save(mockUserRoom);

        Record record = Record.builder()
                .room(mockRoom)
                .user(mockUser)
                .recordType(PAGE)
                .recordPage(150)
                .content("기록")
                .build();
        recordRepository.save(record);

        // when & then
        GlobalException exception = assertThrows(GlobalException.class,
                () -> recordService.deleteRecord(record.getRecordId(), mockUser.getUserId()));
        assertEquals(CANNOT_DELETE_IN_EXPIRED_ROOM, exception.getExceptionStatus());
    }

    @Test
    @DisplayName("기록 작성자가 아닌 경우 삭제 불가")
    void deleteRecordFailNotAuthor() {
        // given
        User otherUser = User.builder()
                .email("other@test.com")
                .nickname("otherUser")
                .password("password")
                .imageUrl("test.jpg")
                .build();
        userRepository.save(otherUser);

        // otherUser를 mockRoom에 추가 (일단 삭제를 시도하려면 방에 속해있어야 하므로)
        UserRoom otherUserRoom = UserRoom.builder()
                .room(mockRoom)
                .user(otherUser)
                .userPercentage(0.0)
                .userRole(HOST)
                .currentPage(0)
                .build();
        userRoomRepository.save(otherUserRoom);

        Record record = Record.builder()
                .room(mockRoom)
                .user(mockUser)
                .recordType(PAGE)
                .recordPage(150)
                .content("기록")
                .build();
        recordRepository.save(record);

        // when & then
        GlobalException exception = assertThrows(GlobalException.class,
                () -> recordService.deleteRecord(record.getRecordId(), otherUser.getUserId()));
        assertEquals(UNAUTHORIZED_DELETE_RECORD, exception.getExceptionStatus());
    }
}