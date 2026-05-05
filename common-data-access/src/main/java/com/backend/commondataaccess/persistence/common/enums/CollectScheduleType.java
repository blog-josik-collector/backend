package com.backend.commondataaccess.persistence.common.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CollectScheduleType {
    CRON("크론 기반 주기 실행"),
    MANUAL("수동 1회성 실행");

    private final String description;

    @JsonValue
    public String getName() {
        return this.name().toLowerCase();
    }

    @JsonCreator
    public static CollectScheduleType from(String name) {
        for (CollectScheduleType collectScheduleType : CollectScheduleType.values()) {
            if (collectScheduleType.name().equalsIgnoreCase(name)) {
                return collectScheduleType;
            }
        }
        return null;
    }
}
