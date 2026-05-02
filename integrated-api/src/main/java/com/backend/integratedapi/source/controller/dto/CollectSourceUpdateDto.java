package com.backend.integratedapi.source.controller.dto;

import com.backend.commondataaccess.persistence.source.enums.ScheduleType;
import com.backend.integratedapi.source.service.dto.CollectSourceDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CollectSourceUpdateDto() {

    public record Request(String url, ScheduleType scheduleType, String cronExpression, Boolean isUsed) {

    }

    @Builder
    public record Response(UUID sourceId, OffsetDateTime updatedAt) {

        public static Response from(CollectSourceDto collectSourceDto) {
            return Response.builder()
                           .sourceId(collectSourceDto.id())
                           .updatedAt(collectSourceDto.updatedAt())
                           .build();
        }
    }
}
