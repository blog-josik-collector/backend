package com.backend.commondataaccess.persistence.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PostReportType {
    /** 포스트 오류 (본문이 비었거나 파싱 실패 등 데이터 자체의 결함) */
    INVALID_CONTENT("포스트 데이터 오류"),
    /** 링크 오류 (404 Not Found, 연결 타임아웃 등 URL 결함) */
    BROKEN_LINK("링크 연결 오류"),
    /** 기타 오류 */
    OTHER("기타 오류");

    private final String description;

    @JsonValue
    public String getName() {
        return this.name().toLowerCase();
    }
}
