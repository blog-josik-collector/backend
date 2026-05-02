package com.backend.commondataaccess.persistence.source.enums;

import static java.util.stream.Collectors.toMap;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Map;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

@Getter
@AllArgsConstructor
public enum ScheduleType {
    CRON(1, "크론 기반 주기 실행"),
    MANUAL(2, "수동 1회성 실행");

    private static final Map<Integer, ScheduleType> ScheduleTypeMap =
            Stream.of(values()).collect(toMap(ScheduleType::getCode, value -> value));

    private final Integer code;
    private final String value;

    public static ScheduleType from(int scheduleTypeCode) {
        ScheduleType scheduleType = ScheduleTypeMap.get(scheduleTypeCode);
        if (ObjectUtils.isEmpty(scheduleType)) {
            throw new IllegalArgumentException("잘못된 ScheduleType 타입입니다.");
        }

        return scheduleType;
    }

    @JsonValue
    public String getName() {
        return this.name().toLowerCase();
    }

    @JsonCreator
    public static ScheduleType from(String name) {
        for (ScheduleType scheduleType : ScheduleType.values()) {
            if (scheduleType.name().equalsIgnoreCase(name)) {
                return scheduleType;
            }
        }
        return null;
    }
}
