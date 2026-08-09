package com.backend.interactionservice.postreport.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.interactionservice.postreport.service.dto.PostReportDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostReportUpdateDto() {

    @Schema(description = "게시글 신고 상태 변경 요청")
    public record Request(
            @Schema(description = "변경할 처리 상태. pending 상태의 신고만 resolved_deleted 또는 rejected_keep 으로 변경할 수 있다")
            ReportStatus status) {
    }

    @Schema(description = "게시글 신고 상태 변경 결과")
    @Builder
    public record Response(
            @Schema(description = "신고 ID")
            UUID id,

            @Schema(description = "변경된 처리 상태")
            ReportStatus status,

            @Schema(description = "변경 시각")
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
