package com.backend.commondataaccess.persistence.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CollectingStatus {
    DISCOVERED("URL이 존재하는 경우"),
    FETCHED("HTML/본문 확보"),
    FETCH_FAILED("HTML/본문 확보 실패"),
    PARSED("정제 텍스트/메타 데이터 추출 완료"),
    PARSE_FAILED("정제 텍스트/메타 데이터 추출 실패");

    private final String description;
}
