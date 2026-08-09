package com.backend.integratedapi.collectsource.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.CollectScheduleType;
import com.backend.integratedapi.collectsource.service.dto.CollectSourceDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CollectSourceReadDto() {

    @Schema(description = "수집 소스 조회 결과")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record Response(
            @Schema(description = "수집 소스 ID")
            UUID sourceId,

            @Schema(description = "연결 Provider ID")
            UUID providerId,

            @Schema(description = "수집 대상 URL")
            String url,

            @Schema(description = "수집 스케줄 유형")
            CollectScheduleType scheduleType,

            @Schema(description = "크론 표현식")
            String cronExpression,

            @Schema(description = "크론 수집 시작 페이지")
            Integer cronFromPage,

            @Schema(description = "크론 수집 종료 페이지")
            Integer cronToPage,

            @Schema(description = "사용 여부")
            boolean isUsed,

            @Schema(description = "생성 시각")
            OffsetDateTime createdAt,

            @Schema(description = "최종 수정 시각")
            OffsetDateTime updatedAt) {

        public static CollectSourceReadDto.Response from(CollectSourceDto collectSourceDto) {
            return CollectSourceReadDto.Response.builder()
                                                .sourceId(collectSourceDto.id())
                                                .providerId(collectSourceDto.providerId())
                                                .url(collectSourceDto.url())
                                                .scheduleType(collectSourceDto.collectScheduleType())
                                                .cronExpression(collectSourceDto.cronExpression())
                                                .cronFromPage(collectSourceDto.cronFromPage())
                                                .cronToPage(collectSourceDto.cronToPage())
                                                .isUsed(collectSourceDto.isUsed())
                                                .createdAt(collectSourceDto.createdAt())
                                                .updatedAt(collectSourceDto.updatedAt())
                                                .build();
        }
    }
}
