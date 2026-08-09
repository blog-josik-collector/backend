package com.backend.integratedapi.collectingjob.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.CollectingStatus;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.integratedapi.collectingjob.service.dto.CollectingJobDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CollectingJobReadDto() {

    @Schema(description = "수집 Job 조회 결과")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record Response(
            @Schema(description = "Job ID")
            UUID jobId,

            @Schema(description = "Job 상태")
            JobStatus jobStatus,

            @Schema(description = "수집 시작 페이지")
            int fromPage,

            @Schema(description = "수집 종료 페이지")
            int toPage,

            @Schema(description = "수집 진행 상태")
            CollectingStatus collectingStatus,

            @Schema(description = "실행 요청자 사용자 ID")
            UUID triggeredBy,

            @Schema(description = "수집 대상 전체 건수")
            int totalCount,

            @Schema(description = "실제 수집된 건수")
            int collectedCount,

            @Schema(description = "시도 횟수")
            int attemptCount,

            @Schema(description = "강제 재수집 여부")
            boolean forceRecollect,

            @Schema(description = "실패 시 에러 메시지")
            String errorMessage,

            @Schema(description = "시작 시각")
            OffsetDateTime startedAt,

            @Schema(description = "종료 시각")
            OffsetDateTime endedAt) {

        public static Response from(CollectingJobDto collectingJobDto) {
            return CollectingJobReadDto.Response.builder()
                                                .jobId(collectingJobDto.id())
                                                .jobStatus(collectingJobDto.jobStatus())
                                                .fromPage(collectingJobDto.fromPage())
                                                .toPage(collectingJobDto.toPage())
                                                .collectingStatus(collectingJobDto.collectingStatus())
                                                .triggeredBy(collectingJobDto.triggeredBy())
                                                .totalCount(collectingJobDto.totalCount())
                                                .collectedCount(collectingJobDto.collectedCount())
                                                .attemptCount(collectingJobDto.attemptCount())
                                                .forceRecollect(collectingJobDto.forceRecollect())
                                                .errorMessage(collectingJobDto.errorMessage())
                                                .startedAt(collectingJobDto.startedAt())
                                                .endedAt(collectingJobDto.endedAt())
                                                .build();
        }
    }
}
