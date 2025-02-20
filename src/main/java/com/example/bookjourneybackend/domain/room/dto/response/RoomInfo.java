package com.example.bookjourneybackend.domain.room.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class RoomInfo {
    private Long roomId;
    private boolean isPublic;
    private String bookTitle;
    private String authorName;
    private String roomName;
    private int memberCount;
    private int recruitCount;
    private Integer roomPercentage;
    private String progressStartDate;
    private String progressEndDate;
    private String imageUrl;
    private boolean isMember;
}