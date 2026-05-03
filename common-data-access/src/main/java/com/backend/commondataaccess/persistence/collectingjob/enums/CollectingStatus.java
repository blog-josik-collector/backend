package com.backend.commondataaccess.persistence.collectingjob.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CollectingStatus {
    DISCOVERED("URL이 존재하는 경우"),
    FETCHED("HTML/본문 확보"),
    PARSED("정제 텍스트/메타 추출 완료");

    private final String description;
}
