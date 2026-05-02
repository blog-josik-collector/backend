package com.backend.integratedapi.collectsource.service.validator;

import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.enums.ScheduleType;
import com.backend.integratedapi.collectsource.service.dto.CollectSourceDto;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectSourceValidator {

    public static UnaryOperator<CollectSourceDto> validateProviderId() {
        return collectSourceDto -> {
            validateProviderId(collectSourceDto.providerId());
            return collectSourceDto;
        };
    }

    public static UnaryOperator<CollectSourceDto> validateScheduleType() {
        return collectSourceDto -> {
            validateScheduleType(collectSourceDto.scheduleType());
            return collectSourceDto;
        };
    }

    public static UnaryOperator<CollectSourceDto> validateId() {
        return collectSourceDto -> {
            validateId(collectSourceDto.id());
            return collectSourceDto;
        };
    }

    public static void validateProviderId(UUID providerId) {
        if (ObjectUtils.isEmpty(providerId)) {
            throw new IllegalArgumentException("provider_id는 필수 입력값입니다.");
        }
    }

    public static void validateScheduleType(ScheduleType scheduleType) {
        if (ObjectUtils.isEmpty(scheduleType)) {
            throw new IllegalArgumentException("schedule_type은 필수 입력값입니다.");
        }
    }

    public static void validateId(UUID id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("id는 필수 입력값입니다.");
        }
    }

    public static void validateScheduleTypeAndCronExpressionPair(ScheduleType scheduleType, String cronExpression) {
        validateScheduleType(scheduleType);

        if (scheduleType.equals(ScheduleType.MANUAL) && StringUtils.isNotBlank(cronExpression)) {
            throw new IllegalArgumentException("schedule_type이 manual일 때 cron_expression은 입력할 수 없습니다.");
        }

        if (scheduleType.equals(ScheduleType.CRON) && StringUtils.isBlank(cronExpression)) {
            throw new IllegalArgumentException("schedule_type이 cron일 때 cron_expression은 필수입니다.");
        }

    }

    public static CollectSource getCollectSourceOrThrow(UUID id, Function<UUID, Optional<CollectSource>> findById) {
        validateId(id);

        return findById.apply(id)
                       .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 collectSource입니다. id: " + id));
    }
}
