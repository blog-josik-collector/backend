package com.backend.integratedapi.indexingjob.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.integratedapi.indexingjob.service.dto.IndexingJobDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record IndexingJobReadDto() {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record Response(UUID jobId,
                           JobStatus jobStatus,
                           IndexingJobType indexingJobType,
                           UUID triggeredBy,
                           UUID targetSourceId,
                           UUID targetPostId,
                           int totalCount,
                           int indexedCount,
                           String errorMessage,
                           OffsetDateTime startedAt,
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
