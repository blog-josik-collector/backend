package com.backend.integratedapi.collectingjob.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.integratedapi.collectingjob.service.dto.CollectingJobDto;
import java.util.UUID;
import lombok.Builder;

public record CollectingJobStartDto() {

    @Builder
    public record Response(UUID jobId, JobStatus jobStatus) {

        public static Response from(CollectingJobDto collectingJobDto) {
            return CollectingJobStartDto.Response.builder()
                                                 .jobId(collectingJobDto.id())
                                                 .jobStatus(collectingJobDto.jobStatus())
                                                 .build();
        }
    }
}
