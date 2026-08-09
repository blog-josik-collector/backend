package com.backend.interactionservice.postreport.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.interactionservice.postreport.service.dto.PostReportDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostReportCreateDto() {

    @Schema(description = "게시글 신고 등록 요청")
    public record Request(
            @Schema(description = "신고 유형")
            PostReportType reportType,

            @Schema(description = "신고 상세 사유", example = "링크가 깨져 있습니다.")
            String content) {

    }

    @Schema(description = "게시글 신고 등록 결과")
    @Builder
    public record Response(
            @Schema(description = "생성된 신고 ID")
            UUID id,

            @Schema(description = "신고 접수 시각")
            OffsetDateTime createdAt) {

        public static Response from(PostReportDto dto) {
            return Response.builder()
                           .id(dto.id())
                           .createdAt(dto.createdAt())
                           .build();
        }
    }
}
