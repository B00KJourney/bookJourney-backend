package com.example.bookjourneybackend.domain.recentSearch.domain.repository;

import com.example.bookjourneybackend.domain.recentSearch.domain.RecentSearch;
import com.example.bookjourneybackend.domain.user.domain.User;
import com.example.bookjourneybackend.domain.user.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@ActiveProfiles("test")
public class RecentSearchRepositoryTest {

    @Autowired
    private RecentSearchRepository recentSearchRepository;

    @Autowired
    private UserRepository userRepository;

    private final String recentSearch1 = "해리포터";
    private final String recentSearch2 = "주술회전";
    private final String recentSearch3 = "한강";

    @Test
    @DisplayName("최근 검색어 추가 및 조회 테스트")
    void saveAndFindRecentSearch() {
        // given
        User user = userRepository.save(User.builder()
                .email("test@example.com")
                .password("verysecret123")
                .nickname("tester")
                .imageUrl("image.jpg")
                .build());

        RecentSearch recentSearch = recentSearchRepository.save(RecentSearch.builder()
                .user(user)
                .recentSearch(recentSearch1)
                .build());

        // when
        List<RecentSearch> recentSearchList = recentSearchRepository.findByUser(user).orElse(List.of());

        // then
        assertThat(recentSearchList).hasSize(1);
        assertThat(recentSearchList.get(0).getRecentSearch()).isEqualTo(recentSearch1);
    }

    @Test
    @DisplayName("특정 최근 검색어 조회 테스트")
    void findSpecificRecentSearch() {
        // given
        User user = userRepository.save(User.builder()
                .email("test2@example.com")
                .password("verysecret123")
                .nickname("tester2")
                .imageUrl("image2.jpg")
                .build());

        recentSearchRepository.save(RecentSearch.builder()
                .user(user)
                .recentSearch(recentSearch2)
                .build());

        // when
        Optional<RecentSearch> recentSearch = recentSearchRepository.findByUserAndRecentSearch(user, recentSearch2);

        // then
        assertThat(recentSearch).isPresent();
        assertThat(recentSearch.get().getRecentSearch()).isEqualTo(recentSearch2);
    }

    @Test
    @DisplayName("최근 검색어 삭제 테스트")
    void deleteRecentSearch() {
        // given
        User user = userRepository.save(User.builder()
                .email("test3@example.com")
                .password("verysecret123")
                .nickname("tester3")
                .imageUrl("image3.jpg")
                .build());

        RecentSearch recentSearch = recentSearchRepository.save(RecentSearch.builder()
                .user(user)
                .recentSearch(recentSearch3)
                .build());

        // when
        recentSearchRepository.delete(recentSearch);
        List<RecentSearch> recentSearchList = recentSearchRepository.findByUser(user).orElse(List.of());

        // then
        assertThat(recentSearchList).isEmpty();
    }

    @Test
    @DisplayName("가장 오래된 검색어 찾기 테스트")
    void findOldestRecentSearch() {
        // given
        User user = userRepository.save(User.builder()
                .email("test4@example.com")
                .password("verysecret123")
                .nickname("tester4")
                .imageUrl("image4.jpg")
                .build());

        recentSearchRepository.save(RecentSearch.builder()
                .user(user)
                .recentSearch(recentSearch1)
                .build());

        RecentSearch oldest = recentSearchRepository.save(RecentSearch.builder()
                .user(user)
                .recentSearch(recentSearch2)
                .build());

        // when
        Optional<RecentSearch> oldestRecentSearch = recentSearchRepository.findTop1ByUserOrderByModifiedAtAsc(user);

        // then
        assertThat(oldestRecentSearch).isPresent();
        assertThat(oldestRecentSearch.get().getRecentSearch()).isEqualTo(recentSearch1);
    }

    @Test
    @DisplayName("최근 검색어 개수 확인 테스트")
    void countRecentSearches() {
        // given
        User user = userRepository.save(User.builder()
                .email("test5@example.com")
                .password("verysecret123")
                .nickname("tester5")
                .imageUrl("image5.jpg")
                .build());

        recentSearchRepository.save(RecentSearch.builder()
                .user(user)
                .recentSearch(recentSearch2)
                .build());
        recentSearchRepository.save(RecentSearch.builder()
                .user(user)
                .recentSearch(recentSearch3)
                .build());

        // when
        Optional<Integer> count = recentSearchRepository.countRecentSearchByUser(user);

        // then
        assertThat(count).isPresent();
        assertThat(count.get()).isEqualTo(2);
    }
}
