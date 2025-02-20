package com.example.bookjourneybackend.domain.favorite.domain.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Getter
@NoArgsConstructor
@AllArgsConstructor(access = PRIVATE)
public class DeleteFavoriteSelectedRequest {

    private List<FavoriteInfo> favoriteList;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor(access = PRIVATE)
    private static class FavoriteInfo {
        private Long favoriteId;

    }

    public List<Long> getFavoriteIds() {
        return favoriteList.stream()
                .map(FavoriteInfo::getFavoriteId)
                .toList();
    }

}
