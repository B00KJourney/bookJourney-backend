package com.example.bookjourneybackend.domain.record.dto.request;

import com.example.bookjourneybackend.domain.record.domain.RecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PostRecordRequest {

    @NotNull(message = "기록의 종류는 필수 입력값입니다.")
    private String recordType;

    private String recordTitle; // 전체 기록일 때 필수
    private Integer recordPage; // 페이지 기록일 때 필수

    @NotBlank(message = "기록 내용은 필수 입력값입니다.")
    private String content;

}
