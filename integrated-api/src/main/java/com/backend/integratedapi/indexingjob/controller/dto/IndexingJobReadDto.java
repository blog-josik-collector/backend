package com.backend.integratedapi.indexingjob.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.integratedapi.indexingjob.service.dto.IndexingJobDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record IndexingJobReadDto() {

    @Schema(description = "색인 Job 조회 결과")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record Response(
            @Schema(description = "Job ID")
            UUID jobId,

            @Schema(description = "Job 상태")
            JobStatus jobStatus,

            @Schema(description = "색인 Job 유형")
            IndexingJobType indexingJobType,

            @Schema(description = "실행 요청자 사용자 ID")
            UUID triggeredBy,

            @Schema(description = "대상 수집 소스 ID")
            UUID targetSourceId,

            @Schema(description = "대상 게시글 ID")
            UUID targetPostId,

            @Schema(description = "색인 대상 전체 건수")
            int totalCount,

            @Schema(description = "실제 색인된 건수")
            int indexedCount,

            @Schema(description = "실패 시 에러 메시지")
            String errorMessage,

            @Schema(description = "시작 시각")
            OffsetDateTime startedAt,

            @Schema(description = "종료 시각")
            OffsetDateTime endedAt) {

        public static Response from(IndexingJobDto indexingJobDto) {
            return IndexingJobReadDto.Response.builder()
                                              .jobId(indexingJobDto.id())
                                              .jobStatus(indexingJobDto.jobStatus())
                                              .indexingJobType(indexingJobDto.indexingJobType())
                                              .triggeredBy(indexingJobDto.triggeredBy())
                                              .targetSourceId(indexingJobDto.targetSourceId())
                                              .targetPostId(indexingJobDto.targetPostId())
                                              .totalCount(indexingJobDto.totalCount())
                                              .indexedCount(indexingJobDto.indexedCount())
                                              .errorMessage(indexingJobDto.errorMessage())
                                              .startedAt(indexingJobDto.startedAt())
                                              .endedAt(indexingJobDto.endedAt())
                                              .build();
        }
    }
}
