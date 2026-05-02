package com.backend.integratedapi.source.controller.dto;

import com.backend.commondataaccess.persistence.source.enums.ScheduleType;
import com.backend.integratedapi.source.service.dto.CollectSourceDto;
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
