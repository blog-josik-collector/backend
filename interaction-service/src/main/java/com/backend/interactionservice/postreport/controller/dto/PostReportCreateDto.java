package com.backend.interactionservice.postreport.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.interactionservice.postreport.service.dto.PostReportDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostReportCreateDto() {

    public record Request(PostReportType reportType,
                          String content) {

    }

    @Builder
    public record Response(UUID id,
                           OffsetDateTime createdAt) {

        public static Response from(PostReportDto dto) {
            return Response.builder()
                           .id(dto.id())
                           .createdAt(dto.createdAt())
                           .build();
        }
    }
}
