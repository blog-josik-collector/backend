package com.backend.interactionservice.postreport.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.interactionservice.postreport.service.dto.PostReportDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostReportUpdateDto() {

    public record Request(ReportStatus status) {
    }

    @Builder
    public record Response(UUID id,
                           ReportStatus status,
                           OffsetDateTime updatedAt) {

        public static Response from(PostReportDto dto) {
            return Response.builder()
                           .id(dto.id())
                           .status(dto.status())
                           .updatedAt(dto.updatedAt())
                           .build();
        }
    }
}
