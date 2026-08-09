package com.backend.integratedapi.collectsource.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.CollectScheduleType;
import com.backend.integratedapi.collectsource.service.dto.CollectSourceDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CollectSourceUpdateDto() {

    @Schema(description = "수집 소스 수정 요청")
    public record Request(
            @Schema(description = "수집 대상 URL")
            String url,

            @Schema(description = "수집 스케줄 유형")
            CollectScheduleType collectScheduleType,

            @Schema(description = "크론 표현식")
            String cronExpression,

            @Schema(description = "크론 수집 시작 페이지")
            Integer cronFromPage,

            @Schema(description = "크론 수집 종료 페이지")
            Integer cronToPage,

            @Schema(description = "사용 여부")
            Boolean isUsed) {

    }

    @Schema(description = "수집 소스 수정 결과")
    @Builder
    public record Response(
            @Schema(description = "수집 소스 ID")
            UUID sourceId,

            @Schema(description = "수정 시각")
            OffsetDateTime updatedAt) {

        public static Response from(CollectSourceDto collectSourceDto) {
            return Response.builder()
                           .sourceId(collectSourceDto.id())
                           .updatedAt(collectSourceDto.updatedAt())
                           .build();
        }
    }
}
