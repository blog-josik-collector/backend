package com.backend.interactionservice.commentreport.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.interactionservice.commentreport.service.dto.CommentReportDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CommentReportUpdateDto() {

    public record Request(ReportStatus status) {

    }

    @Builder
    public record Response(UUID id,
                           ReportStatus status,
                           OffsetDateTime updatedAt) {

        public static Response from(CommentReportDto dto) {
            return Response.builder()
                           .id(dto.id())
                           .status(dto.status())
                           .updatedAt(dto.updatedAt())
                           .build();
        }
    }
}
