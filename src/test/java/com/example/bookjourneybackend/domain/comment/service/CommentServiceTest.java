package com.example.bookjourneybackend.domain.comment.service;

import com.example.bookjourneybackend.domain.book.domain.Book;
import com.example.bookjourneybackend.domain.book.domain.repository.BookRepository;
import com.example.bookjourneybackend.domain.comment.domain.Comment;
import com.example.bookjourneybackend.domain.comment.domain.dto.request.PostCommentRequest;
import com.example.bookjourneybackend.domain.comment.domain.dto.response.GetCommentListResponse;
import com.example.bookjourneybackend.domain.comment.domain.dto.response.PostCommentLikeResponse;
import com.example.bookjourneybackend.domain.comment.domain.dto.response.PostCommentResponse;
import com.example.bookjourneybackend.domain.comment.domain.repository.CommentLikeRepository;
import com.example.bookjourneybackend.domain.comment.domain.repository.CommentRepository;
import com.example.bookjourneybackend.domain.record.domain.Record;
import com.example.bookjourneybackend.domain.record.domain.repository.RecordRepository;
import com.example.bookjourneybackend.domain.room.domain.Room;
import com.example.bookjourneybackend.domain.room.domain.repository.RoomRepository;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.domain.user.domain.repository.UserRepository;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRoom;
import com.example.bookjourneybackend.domain.userRoom.domain.repository.UserRoomRepository;
import com.example.bookjourneybackend.global.exception.GlobalException;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static com.example.bookjourneybackend.domain.book.domain.GenreType.NOVEL_POETRY_DRAMA;
import static com.example.bookjourneybackend.domain.record.domain.RecordType.ENTIRE;
import static com.example.bookjourneybackend.domain.record.domain.RecordType.PAGE;
import static com.example.bookjourneybackend.domain.room.domain.RoomType.TOGETHER;
import static com.example.bookjourneybackend.domain.userRoom.domain.UserRole.HOST;
import static com.example.bookjourneybackend.global.entity.EntityStatus.EXPIRED;
import static com.example.bookjourneybackend.global.response.status.BaseExceptionResponseStatus.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
class CommentServiceTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private UserRoomRepository userRoomRepository;

    @Autowired
    private EntityManager em;

    private User mockUser;
    private Book mockBook;
    private Room mockRoom;
    private UserRoom mockUserRoom;
    private Record mockRecord;

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

        mockRecord = Record.builder()
                .room(mockRoom)
                .user(mockUser)
                .recordType(ENTIRE)
                .recordTitle("기록 테스트 제목")
                .content("기록 테스트 내용")
                .build();
        recordRepository.save(mockRecord);

        em.flush();
        em.clear();
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        recordRepository.deleteAll();
        bookRepository.deleteAll();
        userRoomRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("특정 기록의 댓글을 정상적으로 조회하는 경우")
    void showCommentsSuccess() {
        // given
        Record record = Record.builder()
                .room(mockRoom)
                .user(mockUser)
                .recordType(PAGE)
                .recordPage(150)
                .content("기록 내용")
                .build();
        recordRepository.save(record);
        em.flush();
        em.clear();

        Comment comment1 = Comment.builder()
                .record(record)
                .user(mockUser)
                .content("첫번째 댓글")
                .build();
        commentRepository.save(comment1);

        Comment comment2 = Comment.builder()
                .record(record)
                .user(mockUser)
                .content("두번째 댓글")
                .build();
        commentRepository.save(comment2);

        em.flush();
        em.clear();

        // when
        GetCommentListResponse response = commentService.showComments(record.getRecordId(), mockUser.getUserId());

        // then
        assertNotNull(response);
        assertThat(response.getComments()).isNotNull();  // 댓글 리스트가 null이 아님
        assertFalse(response.getComments().isEmpty());  // 댓글 리스트가 비어있지 않음
        assertThat(response.getComments().size()).isEqualTo(2); // 두 개의 댓글이 존재
        assertThat(response.getComments().get(0).getContent()).isEqualTo("첫번째 댓글");
        assertThat(response.getComments().get(1).getContent()).isEqualTo("두번째 댓글");
    }

    @Test
    @DisplayName("댓글을 정상적으로 작성하는 경우")
    void createCommentSuccess() {
        // given
        PostCommentRequest request = new PostCommentRequest("댓글 내용");

        // when
        PostCommentResponse response = commentService.createComment(mockRecord.getRecordId(), mockUser.getUserId(), request);

        // then
        assertNotNull(response);
        assertThat(response.getCommentId()).isNotNull();
        assertThat(commentRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("방에 속해있지 않은 경우 댓글 작성 불가")
    void createCommentFailNotInRoom() {
        // given
        // 방에 속하지 않은 다른 유저 생성
        User otherUser = User.builder()
                .email("other@test.com")
                .nickname("otherUser")
                .password("password")
                .imageUrl("test.jpg")
                .build();
        userRepository.save(otherUser);

        PostCommentRequest request = new PostCommentRequest("방에 속하지 않은 유저가 댓글 작성 시도");

        // when & then
        GlobalException exception = assertThrows(GlobalException.class,
                () -> commentService.createComment(mockRecord.getRecordId(), otherUser.getUserId(), request));
        assertEquals(CANNOT_FOUND_USER_ROOM, exception.getExceptionStatus());
    }

    @Test
    @DisplayName("방이 EXPIRED 상태라서 댓글을 작성할 수 없는 경우")
    void createCommentFailRoomExpired() {
        // given
        mockUserRoom.setStatus(EXPIRED);
        userRoomRepository.save(mockUserRoom);
        PostCommentRequest request = new PostCommentRequest("댓글 내용");

        // when & then
        GlobalException exception = assertThrows(GlobalException.class,
                () -> commentService.createComment(mockRecord.getRecordId(), mockUser.getUserId(), request));
        assertEquals(CANNOT_COMMENT_IN_EXPIRED_ROOM, exception.getExceptionStatus());
    }

    @Test
    @DisplayName("댓글 좋아요를 누르는 경우")
    void toggleCommentLikeSuccess() {
        // given
        Comment comment = Comment.builder()
                .record(mockRecord)
                .user(mockUser)
                .content("댓글")
                .build();
        commentRepository.save(comment);
        em.flush();
        em.clear();

        // when
        PostCommentLikeResponse response = commentService.toggleCommentLike(comment.getCommentId(), mockUser.getUserId());

        // then
        assertTrue(response.isLiked());
        assertThat(commentLikeRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("댓글 좋아요를 취소하는 경우")
    void toggleCommentLikeCancel() {
        // given
        Comment comment = Comment.builder()
                .record(mockRecord)
                .user(mockUser)
                .content("댓글")
                .build();
        commentRepository.save(comment);

        commentService.toggleCommentLike(comment.getCommentId(), mockUser.getUserId());
        assertThat(commentLikeRepository.count()).isEqualTo(1);

        // when
        PostCommentLikeResponse response = commentService.toggleCommentLike(comment.getCommentId(), mockUser.getUserId());

        // then
        assertFalse(response.isLiked());
        assertThat(commentLikeRepository.count()).isEqualTo(0);
    }


    @Test
    @DisplayName("방에 속해있지 않은 경우 좋아요 불가")
    void toggleCommentLikeFailNotInRoom() {
        // given
        User otherUser = User.builder()
                .email("other@test.com")
                .nickname("otherUser")
                .password("password")
                .imageUrl("test.jpg")
                .build();
        userRepository.save(otherUser);

        Comment comment = Comment.builder()
                .record(mockRecord)
                .user(mockUser)
                .content("댓글")
                .build();
        commentRepository.save(comment);

        // when & then
        GlobalException exception = assertThrows(GlobalException.class,
                () -> commentService.toggleCommentLike(comment.getCommentId(), otherUser.getUserId()));
        assertEquals(NOT_PARTICIPATING_IN_ROOM, exception.getExceptionStatus());
    }


    @Test
    @DisplayName("방이 EXPIRED 상태이면 좋아요 토글 불가")
    void toggleCommentLikeFailExpiredRoom() {
        // given
        mockRoom.setStatus(EXPIRED);
        roomRepository.save(mockRoom);
        em.flush();
        em.clear();

        Comment comment = Comment.builder()
                .record(mockRecord)
                .user(mockUser)
                .content("댓글")
                .build();
        commentRepository.save(comment);

        // when & then
        GlobalException exception = assertThrows(GlobalException.class,
                () -> commentService.toggleCommentLike(comment.getCommentId(), mockUser.getUserId()));
        assertEquals(CANNOT_LIKE_IN_EXPIRED_ROOM, exception.getExceptionStatus());
    }

    @Test
    @DisplayName("댓글을 정상적으로 삭제하는 경우")
    void deleteCommentSuccess() {
        // given
        Comment comment = Comment.builder()
                .record(mockRecord)
                .user(mockUser)
                .content("댓글")
                .build();
        commentRepository.save(comment);
        assertThat(commentRepository.count()).isEqualTo(1);

        // when
        commentService.deleteComment(comment.getCommentId(), mockUser.getUserId());

        // then
        assertThat(commentRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("댓글 작성자가 아닌 경우 삭제 불가")
    void deleteCommentFailNotAuthor() {
        // given
        User otherUser = User.builder()
                .email("other@test.com")
                .nickname("otherUser")
                .password("password")
                .imageUrl("test.jpg")
                .build();
        userRepository.save(otherUser);

        // 다른 유저를 방에 추가 (삭제하려면 방에 있어야 하므로)
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
        em.flush();
        em.clear();

        Comment comment = Comment.builder()
                .record(record)
                .user(mockUser) // 원래 댓글 작성자
                .content("댓글 내용")
                .build();
        commentRepository.save(comment);
        em.flush();
        em.clear();

        // when & then
        GlobalException exception = assertThrows(GlobalException.class,
                () -> commentService.deleteComment(comment.getCommentId(), otherUser.getUserId()));
        assertEquals(UNAUTHORIZED_DELETE_COMMENT, exception.getExceptionStatus());
    }

    @Test
    @DisplayName("방이 EXPIRED 상태이면 댓글 삭제 불가")
    void deleteCommentFailExpiredRoom() {
        // given
        mockUserRoom.setStatus(EXPIRED);
        userRoomRepository.save(mockUserRoom);

        Comment comment = Comment.builder()
                .record(mockRecord)
                .user(mockUser)
                .content("댓글")
                .build();
        commentRepository.save(comment);

        // when & then
        GlobalException exception = assertThrows(GlobalException.class,
                () -> commentService.deleteComment(comment.getCommentId(), mockUser.getUserId()));
        assertEquals(CANNOT_DELETE_IN_EXPIRED_ROOM, exception.getExceptionStatus());
    }
}