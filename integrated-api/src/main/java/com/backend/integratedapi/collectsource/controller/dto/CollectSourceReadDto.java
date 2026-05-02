package com.backend.integratedapi.collectsource.controller.dto;

import com.backend.commondataaccess.persistence.collectsource.enums.ScheduleType;
import com.backend.integratedapi.collectsource.service.dto.CollectSourceDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CollectSourceReadDto() {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record Response(UUID sourceId,
                           UUID providerId,
                           String url,
                           ScheduleType scheduleType,
                           String cronExpression,
                           boolean isUsed,
                           OffsetDateTime createdAt,
                           OffsetDateTime updatedAt) {

        public static CollectSourceReadDto.Response from(CollectSourceDto collectSourceDto) {
            return CollectSourceReadDto.Response.builder()
                                                .sourceId(collectSourceDto.id())
                                                .providerId(collectSourceDto.providerId())
                                                .url(collectSourceDto.url())
                                                .scheduleType(collectSourceDto.scheduleType())
                                                .cronExpression(collectSourceDto.cronExpression())
                                                .isUsed(collectSourceDto.isUsed())
                                                .createdAt(collectSourceDto.createdAt())
                                                .updatedAt(collectSourceDto.updatedAt())
                                                .build();
        }
    }
}
