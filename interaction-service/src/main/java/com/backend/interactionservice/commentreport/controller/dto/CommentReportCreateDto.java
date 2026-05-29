package com.backend.interactionservice.commentreport.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.CommentReportType;
import com.backend.interactionservice.commentreport.service.dto.CommentReportDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CommentReportCreateDto() {

    public record Request(CommentReportType reportType, String content) {

    }

    @Builder
    public record Response(UUID id,
                           OffsetDateTime createdAt) {

        public static Response from(CommentReportDto dto) {
            return Response.builder()
                           .id(dto.id())
                           .createdAt(dto.createdAt())
                           .build();
        }
    }
}
