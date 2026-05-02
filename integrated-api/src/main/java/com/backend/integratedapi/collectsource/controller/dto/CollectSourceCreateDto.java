package com.backend.integratedapi.collectsource.controller.dto;

import com.backend.commondataaccess.persistence.collectsource.enums.ScheduleType;
import com.backend.integratedapi.collectsource.service.dto.CollectSourceDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CollectSourceCreateDto() {

    public record Request(UUID providerId, String url, ScheduleType scheduleType, String cronExpression) {

    }

    @Builder
    public record Response(UUID sourceId, OffsetDateTime createdAt) {

        public static Response from(CollectSourceDto collectSourceDto) {
            return Response.builder()
                           .sourceId(collectSourceDto.id())
                           .createdAt(collectSourceDto.createdAt())
                           .build();
        }
    }
}
