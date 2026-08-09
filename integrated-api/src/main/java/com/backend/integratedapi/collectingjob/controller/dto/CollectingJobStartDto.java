package com.backend.integratedapi.collectingjob.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.integratedapi.collectingjob.service.dto.CollectingJobDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.Builder;

public record CollectingJobStartDto() {

    @Schema(description = "수집 Job 시작 결과")
    @Builder
    public record Response(
            @Schema(description = "생성된 Job ID")
            UUID jobId,

            @Schema(description = "Job 상태")
            JobStatus jobStatus) {

        public static Response from(CollectingJobDto collectingJobDto) {
            return CollectingJobStartDto.Response.builder()
                                                 .jobId(collectingJobDto.id())
                                                 .jobStatus(collectingJobDto.jobStatus())
                                                 .build();
        }
    }
}
