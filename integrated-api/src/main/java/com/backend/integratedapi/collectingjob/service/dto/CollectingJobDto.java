package com.backend.integratedapi.collectingjob.service.dto;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.common.enums.CollectingStatus;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
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
    private JobStatus jobStatus;
    private int fromPage;
    private int toPage;
    private CollectingStatus collectingStatus;
    private UUID triggeredBy;
    private int totalCount;
    private int collectedCount;
    private int attemptCount;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;

    public static CollectingJobDto of(UUID sourceId, UUID userId, int fromPage, int toPage) {
        return CollectingJobDto.builder()
                               .sourceId(sourceId)
                               .userId(userId)
                               .fromPage(fromPage)
                               .toPage(toPage)
                               .build();
    }

    public static CollectingJobDto from(CollectingJob collectingJob) {
        return CollectingJobDto.builder()
                               .id(collectingJob.id())
                               .jobStatus(collectingJob.jobStatus())
                               .fromPage(collectingJob.fromPage())
                               .toPage(collectingJob.toPage())
                               .collectingStatus(collectingJob.collectingStatus())
                               .triggeredBy(collectingJob.triggeredBy())
                               .totalCount(collectingJob.totalCount())
                               .collectedCount(collectingJob.collectedCount())
                               .attemptCount(collectingJob.attemptCount())
                               .errorMessage(collectingJob.errorMessage())
                               .startedAt(collectingJob.startedAt())
                               .endedAt(collectingJob.endedAt())
                               .build();
    }
}
