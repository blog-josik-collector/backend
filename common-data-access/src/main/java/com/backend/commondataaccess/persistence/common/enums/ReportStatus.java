package com.backend.commondataaccess.persistence.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportStatus {
    PENDING("대기중"),
    RESOLVED_FIXED("오류해결 완료"), // 운영자가 확인해서 오류 조치한 경우(ex. 포스트 문제 있는 거 확인해서 오류 수정한 경우)
    RESOLVED_DELETED("삭제완료"), // 운영자가 확인해서 삭제한 경우
    REJECTED_KEEP("유지(반려)"); // 운영자가 확인했는데, 별다른 조치를 하지 않기로 한 경우

    private final String description;

    @JsonValue
    public String getName() {
        return this.name().toLowerCase();
    }
}
