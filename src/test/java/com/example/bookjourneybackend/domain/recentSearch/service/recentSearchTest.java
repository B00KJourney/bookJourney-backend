package com.example.bookjourneybackend.domain.recentSearch.service;

import com.example.bookjourneybackend.domain.recentSearch.domain.RecentSearch;
import com.example.bookjourneybackend.domain.recentSearch.domain.dto.response.GetRecentSearchResponse;
import com.example.bookjourneybackend.domain.recentSearch.domain.repository.RecentSearchRepository;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.domain.user.domain.repository.UserRepository;
import com.example.bookjourneybackend.global.util.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecentSearchServiceTest {

    @Mock
    private RecentSearchRepository recentSearchRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DateUtil dateUtil;

    @InjectMocks
    private RecentSearchService recentSearchService;

    private User mockUser;
    private RecentSearch mockRecentSearch;

    private final String email = "test@example.com";
    private final String password = "password123";
    private final String nickName = "testUser";
    private final Long userId = 1L;
    private final String recentSearchTerm = "Java";

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .userId(1L)
                .email(email)
                .password(password)
                .nickname(nickName)
                .build();

        mockRecentSearch = RecentSearch.builder()
                .recentSearchId(1L)
                .recentSearch(recentSearchTerm)
                .user(mockUser)
                .build();
    }

    @Test
    @DisplayName("로그인한 유저의 최근 검색어 조회")
    void showRecentSearch() {
        // given
        List<RecentSearch> recentSearchList = List.of(mockRecentSearch);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(recentSearchRepository.findTop12ByUserOrderByModifiedAtDesc(mockUser))
                .thenReturn(Optional.of(recentSearchList));

        // when
        GetRecentSearchResponse response = recentSearchService.showRecentSearch(userId);

        // then
        assertNotNull(response);
        assertEquals(1, response.getRecentSearchList().size());
        assertEquals(recentSearchTerm, response.getRecentSearchList().get(0).getRecentSearch());
    }

    @Test
    @DisplayName("특정 최근 검색어 삭제")
    void deleteRecentSearch() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(recentSearchRepository.findByUserAndRecentSearchId(mockUser, mockRecentSearch.getRecentSearchId()))
                .thenReturn(Optional.of(mockRecentSearch));

        // when
        recentSearchService.deleteRecentSearch(mockRecentSearch.getRecentSearchId(), userId);

        // then
        verify(recentSearchRepository).delete(mockRecentSearch);
    }

    @Test
    @DisplayName("전체 최근 검색어 삭제")
    void deleteRecentSearchAll() {
        // given
        List<RecentSearch> recentSearchList = List.of(mockRecentSearch);
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(recentSearchRepository.findByUser(mockUser)).thenReturn(Optional.of(recentSearchList));

        // when
        recentSearchService.deleteRecentSearchAll(userId);

        // then
        verify(recentSearchRepository).deleteAll(recentSearchList);
    }

    @Test
    @DisplayName("새로운 최근 검색어 추가")
    void addRecentSearch() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(recentSearchRepository.findByUserAndRecentSearch(mockUser, recentSearchTerm))
                .thenReturn(Optional.empty());
        when(recentSearchRepository.countRecentSearchByUser(mockUser)).thenReturn(Optional.of(11));

        // when
        recentSearchService.addRecentSearch(userId, recentSearchTerm);

        // then
        verify(recentSearchRepository).save(any(RecentSearch.class));
    }

    @Test
    @DisplayName("이미 존재하는 최근 검색어가 있으면 modified_at 업데이트")
    void addRecentSearch_existingTerm() {
        // given
        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));
        when(recentSearchRepository.findByUserAndRecentSearch(mockUser, recentSearchTerm))
                .thenReturn(Optional.of(mockRecentSearch));

        // when
        recentSearchService.addRecentSearch(userId, recentSearchTerm);

        // then
        verify(recentSearchRepository, never()).save(any(RecentSearch.class));
        assertEquals(mockRecentSearch.getModifiedAt(), dateUtil.getCurrentTime());
    }
}

