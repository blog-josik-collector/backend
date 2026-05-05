package com.backend.integratedapi.collectsource.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.CollectScheduleType;
import com.backend.integratedapi.collectsource.service.dto.CollectSourceDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CollectSourceUpdateDto() {

    public record Request(String url, CollectScheduleType collectScheduleType, String cronExpression, Boolean isUsed) {

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
