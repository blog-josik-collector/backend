package com.backend.commondataaccess.persistence.source.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ScheduleTypeConverter implements AttributeConverter<ScheduleType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ScheduleType scheduleType) {
        return scheduleType.getCode();
    }

    @Override
    public ScheduleType convertToEntityAttribute(Integer scheduleTypeCode) {
        return ScheduleType.from(scheduleTypeCode);
    }
}
