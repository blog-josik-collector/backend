package com.backend.integratedapi.collectingjob.service.dto;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(fluent = true)
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더용
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // Jackson 역직렬화용
public class CollectingJobDto {

    private UUID userId;
    private UUID sourceId;

    private UUID id;
    private JobStatus status;


    //TODO: userId 추가 예정
    public static CollectingJobDto of(UUID sourceId, UUID userId) {
        return CollectingJobDto.builder()
                               .sourceId(sourceId)
                               .userId(userId)
                               .build();
    }

    public static CollectingJobDto from(CollectingJob collectingJob) {
        return CollectingJobDto.builder()
                               .id(collectingJob.id())
                               .status(collectingJob.jobStatus())
                               .build();
    }
}
