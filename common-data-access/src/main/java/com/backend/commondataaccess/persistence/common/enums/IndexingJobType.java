package com.backend.commondataaccess.persistence.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IndexingJobType {
    CRON("주기적 자동 색인"),
    MANUAL("수동 재색인");

    private final String description;

    @JsonValue
    public String getName() {
        return this.name().toLowerCase();
    }

    @JsonCreator
    public static IndexingJobType from(String name) {
        for (IndexingJobType indexingJobType : IndexingJobType.values()) {
            if (indexingJobType.name().equalsIgnoreCase(name)) {
                return indexingJobType;
            }
        }
        return null;
    }
}
