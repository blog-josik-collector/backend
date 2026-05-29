package com.backend.commondataaccess.persistence.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReportStatus {
    PENDING("대기중"),
    RESOLVED_DELETED("삭제완료"),
    REJECTED_KEEP("유지(반려)");

    private final String description;

    @JsonValue
    public String getName() {
        return this.name().toLowerCase();
    }
}
