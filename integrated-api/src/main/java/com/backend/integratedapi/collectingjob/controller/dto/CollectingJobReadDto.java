package com.backend.integratedapi.collectingjob.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.CollectingStatus;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.integratedapi.collectingjob.service.dto.CollectingJobDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CollectingJobReadDto() {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record Response(UUID jobId,
                           JobStatus jobStatus,
                           CollectingStatus collectingStatus,
                           UUID triggeredBy,
                           int totalCount,
                           int collectedCount,
                           int attemptCount,
                           String errorMessage,
                           OffsetDateTime startedAt,
                           OffsetDateTime endedAt) {

        public static Response from(CollectingJobDto collectingJobDto) {
            return CollectingJobReadDto.Response.builder()
                                                .jobId(collectingJobDto.id())
                                                .jobStatus(collectingJobDto.jobStatus())
                                                .collectingStatus(collectingJobDto.collectingStatus())
                                                .triggeredBy(collectingJobDto.triggeredBy())
                                                .totalCount(collectingJobDto.totalCount())
                                                .collectedCount(collectingJobDto.collectedCount())
                                                .attemptCount(collectingJobDto.attemptCount())
                                                .errorMessage(collectingJobDto.errorMessage())
                                                .startedAt(collectingJobDto.startedAt())
                                                .endedAt(collectingJobDto.endedAt())
                                                .build();
        }
    }
}
