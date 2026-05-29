package com.backend.commondataaccess.persistence.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommentReportType {
    /** 정치적 목적 / 선동 */
    POLITICAL("정치적 목적"),
    /** 성인 / 음란성 */
    ADULT("성인/음란성"),
    /** 기타 사유 */
    OTHER("기타 사유");

    private final String description;

    @JsonValue
    public String getName() {
        return this.name().toLowerCase();
    }
}
