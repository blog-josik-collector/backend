package com.backend.commondataaccess.persistence.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PostCommentStatus {
    ACTIVE("정상"),
    BLOCKED("숨김/차단"),
    DELETED("삭제");

    private final String description;

    @JsonValue
    public String getName() {
        return this.name().toLowerCase();
    }
}
