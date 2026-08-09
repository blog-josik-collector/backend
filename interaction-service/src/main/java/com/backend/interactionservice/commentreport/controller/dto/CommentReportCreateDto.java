package com.backend.interactionservice.commentreport.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.CommentReportType;
import com.backend.interactionservice.commentreport.service.dto.CommentReportDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CommentReportCreateDto() {

    @Schema(description = "댓글 신고 등록 요청")
    public record Request(
            @Schema(description = "신고 유형")
            CommentReportType reportType,

            @Schema(description = "신고 상세 사유", example = "광고성 댓글입니다.")
            String content) {

    }

    @Schema(description = "댓글 신고 등록 결과")
    @Builder
    public record Response(
            @Schema(description = "생성된 신고 ID")
            UUID id,

            @Schema(description = "신고 접수 시각")
            OffsetDateTime createdAt) {

        public static Response from(CommentReportDto dto) {
            return Response.builder()
                           .id(dto.id())
                           .createdAt(dto.createdAt())
                           .build();
        }
    }
}
