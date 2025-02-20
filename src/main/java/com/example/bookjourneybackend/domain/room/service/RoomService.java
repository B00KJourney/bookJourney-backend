package com.example.bookjourneybackend.domain.room.service;

import com.example.bookjourneybackend.domain.book.domain.GenreType;
import com.example.bookjourneybackend.domain.favorite.domain.repository.FavoriteRepository;
import com.example.bookjourneybackend.domain.recentSearch.service.RecentSearchService;
import com.example.bookjourneybackend.domain.record.domain.repository.RecordRepository;
import com.example.bookjourneybackend.domain.room.domain.Room;
import com.example.bookjourneybackend.domain.room.domain.SearchType;
import com.example.bookjourneybackend.domain.room.domain.SortType;
import com.example.bookjourneybackend.domain.room.domain.repository.RoomRepository;
import com.example.bookjourneybackend.domain.room.dto.response.*;
import com.example.bookjourneybackend.domain.book.domain.Book;
import com.example.bookjourneybackend.domain.book.domain.repository.BookRepository;
import com.example.bookjourneybackend.domain.room.dto.request.PostRoomCreateRequest;
import com.example.bookjourneybackend.domain.room.dto.response.GetRoomDetailResponse;
import com.example.bookjourneybackend.domain.room.dto.response.GetRoomInfoResponse;
import com.example.bookjourneybackend.domain.room.dto.response.PostRoomCreateResponse;
import com.example.bookjourneybackend.domain.room.dto.response.RoomMemberInfo;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.domain.user.domain.repository.UserRepository;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRole;
import com.example.bookjourneybackend.domain.userRoom.domain.UserRoom;
import com.example.bookjourneybackend.domain.userRoom.domain.repository.UserRoomRepository;
import com.example.bookjourneybackend.global.entity.EntityStatus;
import com.example.bookjourneybackend.global.exception.GlobalException;
import com.example.bookjourneybackend.global.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import com.example.bookjourneybackend.global.util.AladinApiUtil;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.example.bookjourneybackend.domain.room.domain.RoomType.ALONE;
import static com.example.bookjourneybackend.domain.room.domain.RoomType.TOGETHER;
import static com.example.bookjourneybackend.domain.room.domain.SortType.LASTEST;
import static com.example.bookjourneybackend.domain.userRoom.domain.UserRole.HOST;
import static com.example.bookjourneybackend.global.entity.EntityStatus.*;
import static com.example.bookjourneybackend.global.response.status.BaseExceptionResponseStatus.*;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final UserRoomRepository userRoomRepository;
    private final AladinApiUtil aladinApiUtil;
    private final DateUtil dateUtil;
    private final RecordRepository recordRepository;
    private final FavoriteRepository favoriteRepository;

    private final RecentSearchService recentSearchService;

    /**
     * 방 상세정보 조회
     */
    public GetRoomDetailResponse showRoomDetails(Long roomId, Long userId) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(CANNOT_FOUND_ROOM));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(CANNOT_FOUND_USER));

        boolean isFavorite = favoriteRepository.existsActiveFavoriteByUserIdAndBook(userId, room.getBook());

        List<RoomMemberInfo> members = getRoomMemberInfoList(room);
        boolean isMember = userRoomRepository.existsByRoomAndUser(room, user);

        return GetRoomDetailResponse.of(room, isMember, isFavorite, members, dateUtil);

    }

    /**
     * 방 정보 조회
     */
    public GetRoomInfoResponse showRoomInfo(Long roomId, Long userId) {

        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(CANNOT_FOUND_ROOM));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(CANNOT_FOUND_USER));

        List<RoomMemberInfo> members = getRoomMemberInfoList(room);
        boolean isMember = userRoomRepository.existsByRoomAndUser(room, user);

        return GetRoomInfoResponse.of(
                room.getRoomId(),
                room.getBook().getBookTitle(),
                room.getRoomName(),
                room.isPublic(),
                room.getRoomPercentage().intValue(),
                dateUtil.calculateDday(room.getProgressEndDate()),
                isMember,
                room.getRecruitCount(),
                members);
    }

    /**
     * 방 검색
     */
    @Transactional
    public GetRoomSearchResponse searchRooms(
            String searchTerm, String searchType, String genre,
            String recruitStartDate, String recruitEndDate,
            String roomStartDate, String roomEndDate,
            Integer recordCount, Integer page, Long userId
    ) {
        validateSearchParams(searchTerm, searchType, page);

        SearchType searchTypeEnum = SearchType.from(searchType);

        GenreType genreType = genre != null && !genre.isEmpty() ? GenreType.fromGenreType(genre) : null;
        recentSearchService.addRecentSearch(userId, searchTerm);

        // 검색어를 LOWER()로 변환하여 LIKE 검색 수행
        String searchQuery = searchTerm.toLowerCase();

        Slice<Room> rooms = roomRepository.findRoomsByFilters(
                genreType,
                dateUtil.parseDate(recruitStartDate),
                dateUtil.parseDate(recruitEndDate),
                dateUtil.parseDate(roomStartDate),
                dateUtil.parseDate(roomEndDate),
                recordCount,
                searchTypeEnum.name(),
                searchQuery, // LIKE 검색 수행
                PageRequest.of(page, 10)
        );

        List<RoomInfo> roomInfos = rooms.stream()
                .map(room -> mapRoomToRoomInfo(room, userId))
                .toList();

        return GetRoomSearchResponse.of(roomInfos);
    }

    private void validateSearchParams(String searchTerm, String searchType, Integer page) {
        // 필수 값 검증
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            throw new GlobalException(EMPTY_SEARCH_TERM);
        }
        if (searchType == null || searchType.trim().isEmpty()) {
            throw new GlobalException(INVALID_SEARCH_TYPE);
        }
        if (page == null) {
            throw new GlobalException(INVALID_PAGE);
        }
    }

    //Room 객체를 RoomInfo 객체로 매핑
    private RoomInfo mapRoomToRoomInfo(Room room, Long userId) {
        boolean isMember = room.getUserRooms().stream()
                .anyMatch(userRoom -> userRoom.getUser().getUserId().equals(userId));

        return RoomInfo.builder()
                .roomId(room.getRoomId())
                .isPublic(room.isPublic())
                .bookTitle(room.getBook().getBookTitle())
                .authorName(room.getBook().getAuthorName())
                .roomName(room.getRoomName())
                .memberCount(room.getUserRooms().size())
                .recruitCount(room.getRecruitCount())
                .roomPercentage(room.getRoomPercentage().intValue())
                .progressStartDate(dateUtil.formatDate(room.getStartDate()))
                .progressEndDate(dateUtil.formatDate(room.getProgressEndDate()))
                .imageUrl(room.getBook().getImageUrl())
                .isMember(isMember)
                .build();
    }

    //Room 객체의 UserRoom 정보를 RoomMemberInfo 객체로 매핑
    private List<RoomMemberInfo> getRoomMemberInfoList(Room room) {
        return room.getUserRooms().stream()
                .map(userRoom -> {
                    User user = userRoom.getUser();
                    return RoomMemberInfo.builder()
                            .userRole(userRoom.getUserRole())
                            .imageUrl(user.getImageUrl())
                            .nickName(user.getNickname())
                            .userPercentage(userRoom.getUserPercentage().intValue())
                            .build();
                }).collect(Collectors.toList());
    }

    /**
     * 1. Book 테이블에 존재하면, 그대로 매핑
     * 2. Book 테이블에 존재하지 않으면, 알라딘 api를 통해 book 정보를 가져와서 repository에 저장후 매핑
     *
     * @param postRoomCreateRequest, userId
     * @return postRoomCreateResponse
     */
    @Transactional
    public PostRoomCreateResponse createRoom(PostRoomCreateRequest postRoomCreateRequest, Long userId) {
        Book book = findOrCreateBook(postRoomCreateRequest.getIsbn());

        UserRoom userRoom = createUserRoom(userId);
        Room room = createRoom(postRoomCreateRequest, book, userRoom);

        book.addRoom(room);
        bookRepository.save(book);
        roomRepository.save(room); //CascadeType.All 옵션을 제거하고 room도 save (이유 : CascadeType.ALL을 했더니 roomRepository에 메서드가 종료되고 저장되어서 roomId가 null이 뜨는 현상이 발생

        return PostRoomCreateResponse.of(room);
    }

    //isbn을 통해 book을 찾거나, 없을 경우 알라딘 api를 통해 book 정보를 가져와서 저장
    private Book findOrCreateBook(String isbn) {
        return bookRepository.findByIsbn(isbn)
                .map(this::updateBookIfNeeded)
                .orElseGet(() -> saveBookFromAladinApi(isbn));
    }

    //책의 페이지수가 없을 경우, 알라딘 api를 통해 페이지수를 가져와서 업데이트
    private Book updateBookIfNeeded(Book existingBook) {
        if (existingBook.getPageCount() == null) {
            Book updatedBook = saveBookFromAladinApi(existingBook.getIsbn());
            existingBook.setPageCount(updatedBook.getPageCount());
            return bookRepository.save(existingBook);
        }
        return existingBook;
    }

    private Book saveBookFromAladinApi(String isbn) {

        String requestUrl = aladinApiUtil.buildLookUpApiUrl(isbn);
        String currentResponse = aladinApiUtil.requestBookInfoFromAladinApi(requestUrl);

        return aladinApiUtil.parseAladinApiResponseToBook(currentResponse, false, 0);
    }

    //유저 정보를 통해 UserRoom 객체 생성
    private UserRoom createUserRoom(Long userId) {
        User user = userRepository.findByUserIdAndStatus(userId, ACTIVE)
                .orElseThrow(() -> new GlobalException(CANNOT_FOUND_USER));
        return UserRoom.builder()
                .user(user)
                .userRole(HOST)
                .currentPage(0)
                .userPercentage(0.0)
                .build();
    }

    //방 생성
    private Room createRoom(PostRoomCreateRequest request, Book book, UserRoom userRoom) {
        Room room;
        if (request.getRecruitCount() == 1) {
            if (userRoomRepository.existsUnExpiredAloneRoomByUserAndBook(userRoom.getUser().getUserId(), book.getIsbn())) {
                throw new GlobalException(ALREADY_CREATED_ALONE_ROOM);
            }
            room = Room.makeReadAloneRoom(book);
        } else {
            validatedRoomCreateRequest(request);

            LocalDate startDate = dateUtil.parseDateToLocalDateString(request.getProgressStartDate());
            LocalDate progressEndDate = dateUtil.parseDateToLocalDateString(request.getProgressEndDate());
            room = Room.makeReadTogetherRoom(
                    request.getRoomName(), book, request.isPublic(), request.getPassword(),
                    startDate, progressEndDate, dateUtil.calculateRecruitEndDate(startDate, progressEndDate), request.getRecruitCount()
            );
        }
        room.addUserRoom(userRoom);
        return room;
    }

    private void validatedRoomCreateRequest(PostRoomCreateRequest request) {
        // 같이읽기 방에서 startDate, progressEndDate가 null일 경우 예외 처리
        // isPublic이 false이면 password가 null일 경우 예외 처리
        if (request.getProgressStartDate() == null || request.getProgressEndDate() == null) {
            throw new GlobalException(CANNOT_NULL_DATE);
        }

        if (!request.isPublic() && request.getPassword() == null) {
            throw new GlobalException(CANNOT_NULL_PASSWORD);
        }
    }

    /**
     * 최신순, 유저 진행도순 정렬
     * 방이 ACTIVE인 것만 보여줌
     */
    //todo 추후에 예외처리 메시지 수정
    @Transactional(readOnly = true)
    public GetRoomActiveResponse searchActiveRooms(String sort, Long userId) {
        SortType sortType = (sort == null) ? LASTEST : SortType.from(sort);
        List<UserRoom> userRooms = findUserRoomsBySortType(sortType, userId);

        return GetRoomActiveResponse.of(parsingUserRoomsToRecordInfo(userRooms));
    }

    //정렬 타입에 따라 UserRoom 조회
    private List<UserRoom> findUserRoomsBySortType(SortType sortType, Long userId) {
        return switch (sortType) {
            case LASTEST -> userRoomRepository.findUserRoomsOrderByModifiedAt(userId);
            case PROGRESS -> userRoomRepository.findUserRoomsOrderByUserPercentage(userId);
            default -> throw new GlobalException(INVALID_SORT_TYPE);
        };
    }

    //UserRoom을 RecordInfo로 매핑
    private List<RecordInfo> parsingUserRoomsToRecordInfo(List<UserRoom> userRooms) {
        if (userRooms.isEmpty()) {
            throw new GlobalException(CANNOT_FOUND_BOOK);
        }

        return userRooms.stream()
                .map(userRoom -> {
                    Room room = userRoom.getRoom();
                    Book book = room.getBook();
                    return RecordInfo.builder()
                            .roomId(room.getRoomId())
                            .imageUrl(book.getImageUrl())
                            .bookTitle(book.getBookTitle())
                            .authorName(book.getAuthorName())
                            .roomType(room.getRoomType().getRoomType())
                            .modifiedAt(dateUtil.calculateLastActivityTime(room.getRecords()))
                            .userPercentage(userRoom.getUserPercentage())
                            .build();
                }).toList();
    }

    /**
     * 진행중인 기록 삭제
     * UserRoom의 status를 INACTIVE로 변경
     */
    @Transactional
    public Void putRoomsInactive(Long roomId, Long userId) {
        UserRoom userRoom = getUserRoom(roomId, userId);

        userRoom.setStatus(EntityStatus.INACTIVE);
        userRoom.setInActivatedAt(dateUtil.getCurrentTime());
        userRoomRepository.save(userRoom);

        return null;
    }

    private UserRoom getUserRoom(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(CANNOT_FOUND_ROOM));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(CANNOT_FOUND_USER));


        UserRoom userRoom = userRoomRepository.findUserRoomByRoomAndUserAndStatus(room, user, ACTIVE)
                .orElseThrow(() -> new GlobalException(CANNOT_FOUND_USER_ROOM));
        return userRoom;
    }

    /**
     * 혼자읽기일 경우 무조건 Room 삭제
     * 같이읽기일 경우 UserRole= Member면 -> Room 삭제 X
     * 같이읽기일 경우 UserRole= Host면 -> 자신만 남았다면 나갈수 있고 Room 삭제
     */
    @Transactional
    public Void exitRoom(Long roomId, Long userId) {

        UserRoom userRoom = getUserRoom(roomId, userId);
        Room room = userRoom.getRoom();

        if (room.getRoomType() == TOGETHER) {
            handleTogetherRoomExit(userRoom, room);
        }
        if (room.getRoomType() == ALONE) {
            roomRepository.delete(room);    // 혼자읽기 방은 나가면 방 삭제
        }

        return null;
    }

    private void handleTogetherRoomExit(UserRoom userRoom, Room room) {
        if (userRoom.getUserRole() == HOST) {
            removeHostFromRoom(room);
        }
        if (userRoom.getUserRole() == UserRole.MEMBER) {
            removeMemberFromRoom(userRoom, room);
        }
    }

    private void removeHostFromRoom(Room room) {
        if (room.getUserRooms().size() == 1) {
            roomRepository.delete(room);    // 같이읽기 방에서 방장이 혼자 남아 있는 있을때 방을 나가면 방 삭제
        }
        if (room.getUserRooms().size() != 1) {
            throw new GlobalException(HOST_CANNOT_LEAVE_ROOM);  // 같이읽기 방에서 방장이 혼자 남아 있지 않으면 방장은 나갈 수 없음
        }
    }

    private void removeMemberFromRoom(UserRoom userRoom, Room room) {
        recordRepository.deleteAllByRoomAndUser(room, userRoom.getUser());
        userRoomRepository.delete(userRoom);    // 같이읽기 방에서 멤버가 나가도 방 삭제 X (해당 사용자와 관련된 방 정보 삭제)
    }


    /**
     * 방 참여
     */
    @Transactional
    public PostJoinRoomResponse joinRoom(Long roomId, Long userId, Integer password) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new GlobalException(CANNOT_FOUND_ROOM));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new GlobalException(CANNOT_FOUND_USER));

        // 이미 방에 참여한 유저인지 확인
        if (userRoomRepository.existsByRoomAndUser(room, user)) {
            throw new GlobalException(ALREADY_JOINED_ROOM);
        }

        // 모집 기간이 지났는지 확인
        if (LocalDate.now().isAfter(room.getRecruitEndDate())) {
            throw new GlobalException(ROOM_NOT_RECRUITING);
        }

        // 인원 초과 여부 확인
        if (room.getUserRooms().size() >= room.getRecruitCount()) {
            throw new GlobalException(ROOM_FULL);
        }

        // 비공개 방의 경우 비밀번호 확인
        if (!room.isPublic() && !Objects.equals(room.getPassword(), password)) {
            throw new GlobalException(INVALID_ROOM_PASSWORD);
        }

        UserRoom userRoom = UserRoom.builder()
                .user(user)
                .room(room)
                .userRole(UserRole.MEMBER)
                .userPercentage(0.0)
                .currentPage(0)
                .build();
        userRoomRepository.save(userRoom);

        return PostJoinRoomResponse.of(room);
    }

    /**
     * 현재가 몇월 몇째주인지 계산
     * 현재 주차의 첫째날과 마지막날을 구해서 모집마감일이 해당 날 사이에 있는 모든 Room을 찾음
     * <정렬 우선순위>
     * 1. 공개방
     * 2. 댓글+답글 수가 많은 순
     * 3. 모집마감일까지 많이 남은 순
     * 4. 최근에 방 생성된 순
     */
    public GetRoomRecruitmentResponse searchRecruitmentRooms() {

        LocalDate now = LocalDate.now();
        String weekOfMonth = dateUtil.getCurrentWeekOfMonth(now);   //현재 주차를 기준으로 "~월 ~주차 반환"
        LocalDate[] firstAndLastDayOfWeek = dateUtil.getFirstAndLastDayOfWeek(now);

        List<Room> rooms = roomRepository.findRecruitmentRooms(firstAndLastDayOfWeek[0], firstAndLastDayOfWeek[1], PageRequest.of(0, 5));

        return GetRoomRecruitmentResponse.of(
                weekOfMonth,
                rooms.stream()
                        .map(room -> RoomInfo.builder()
                                .roomId(room.getRoomId())
                                .bookTitle(room.getBook().getBookTitle())
                                .authorName(room.getBook().getAuthorName())
                                .roomName(room.getRoomName())
                                .recruitCount(room.getRecruitCount())
                                .memberCount(room.getUserRooms().size())
                                .isPublic(room.isPublic())
                                .progressStartDate(dateUtil.formatDate(room.getStartDate()))
                                .progressEndDate(dateUtil.formatDate(room.getProgressEndDate()))
                                .build())
                        .toList()
        );
    }

    /**
     * 읽은 페이지 입력 시 책 전체 & 저번까지 읽었던 페이지 조회
     */
    @Transactional(readOnly = true)
    public GetRoomPagesResponse showRoomPages(Long roomId, Long userId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new GlobalException(CANNOT_FOUND_ROOM));
        User user = userRepository.findById(userId).orElseThrow(() -> new GlobalException(CANNOT_FOUND_USER));
        UserRoom userRoom = userRoomRepository.findUserRoomByRoomAndUser(room, user).orElseThrow(() -> new GlobalException(CANNOT_FOUND_USER_ROOM));

        int bookPage = room.getBook().getPageCount();
        int currentPage = userRoom.getCurrentPage();

        return GetRoomPagesResponse.of(bookPage, currentPage);

    }

    /**
     * 검색으로 비공개 방 진입 시 필요한 정보들
     * 비밀번호, 방 이름, 호스트 닉네임 반환
     */
    public GetSearchPrivateRoomResponse showSearchPrivateRooms(Long roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(() -> new GlobalException(CANNOT_FOUND_ROOM));

        // 방이 공개이면 예외 발생
        if (room.isPublic()) {
            throw new GlobalException(ROOM_IS_PUBLIC);
        }

        if (room.getRoomType() == ALONE) {
            throw new GlobalException(ROOM_IS_ALONE);
        }

        UserRoom host = room.getUserRooms().stream()
                .filter(userRoom -> userRoom.getUserRole().equals(HOST))
                .findFirst()
                .orElseThrow(() -> new GlobalException(CANNOT_FIND_HOST));

        return GetSearchPrivateRoomResponse.of(
                room.getRoomId(),
                room.getRoomName(),
                host.getUser().getNickname(),
                room.getPassword()
        );
    }

    /**
     * 매일 자정마다 만료된 방 체크
     */
    @Async
    @Transactional
    public void checkExpiredRooms() {
        List<Room> expiredRooms = roomRepository.findByProgressEndDateBefore(dateUtil.getCurrentDate());
        expiredRooms.forEach(room -> {
            List<UserRoom> userRooms = room.getUserRooms();
            userRooms.forEach(userRoom -> {
                userRoom.setStatus(EXPIRED);
                userRoomRepository.save(userRoom);
            });
            room.setStatus(EXPIRED);
            roomRepository.save(room);
        });
    }
}
