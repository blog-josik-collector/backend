package com.backend.integratedapi.collectsource.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.CollectScheduleType;
import com.backend.integratedapi.collectsource.service.dto.CollectSourceDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CollectSourceCreateDto() {

    @Schema(description = "수집 소스 생성 요청")
    public record Request(
            @Schema(description = "연결할 Provider ID")
            UUID providerId,

            @Schema(description = "수집 대상 URL", example = "https://toss.tech/article")
            String url,

            @Schema(description = "수집 스케줄 유형")
            CollectScheduleType scheduleType,

            @Schema(description = "크론 표현식. CRON 타입일 때 사용", example = "0 0 3 * * *")
            String cronExpression,

            @Schema(description = "크론 수집 시작 페이지. CRON 타입일 때 사용", example = "1")
            Integer cronFromPage,

            @Schema(description = "크론 수집 종료 페이지. CRON 타입일 때 사용", example = "3")
            Integer cronToPage) {

    }

    @Schema(description = "수집 소스 생성 결과")
    @Builder
    public record Response(
            @Schema(description = "생성된 수집 소스 ID")
            UUID sourceId,

            @Schema(description = "생성 시각")
            OffsetDateTime createdAt) {

        public static Response from(CollectSourceDto collectSourceDto) {
            return Response.builder()
                           .sourceId(collectSourceDto.id())
                           .createdAt(collectSourceDto.createdAt())
                           .build();
        }
    }
}
