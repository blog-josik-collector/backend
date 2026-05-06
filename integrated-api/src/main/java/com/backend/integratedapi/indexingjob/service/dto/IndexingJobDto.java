package com.backend.integratedapi.indexingjob.service.dto;

import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
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
public class IndexingJobDto {

    private UUID id;
    private IndexingJobType indexingJobType;
    private JobStatus jobStatus;
    private int totalCount;
    private int indexedCount;
    private String errorMessage;
    private OffsetDateTime startedAt;
    private OffsetDateTime endedAt;

    private UUID triggeredBy;
    private UUID targetSourceId;
    private UUID targetPostId;

    public static IndexingJobDto from(IndexingJob indexingJob) {
        return IndexingJobDto.builder()
                             .id(indexingJob.id())
                             .indexingJobType(indexingJob.indexingJobType())
                             .jobStatus(indexingJob.jobStatus())
                             .totalCount(indexingJob.totalCount())
                             .indexedCount(indexingJob.indexedCount())
                             .errorMessage(indexingJob.errorMessage())
                             .startedAt(indexingJob.startedAt())
                             .endedAt(indexingJob.endedAt())
                             .triggeredBy(indexingJob.triggeredBy())
                             .targetSourceId(indexingJob.targetSource() == null ? null : indexingJob.targetSource().id())
                             .targetPostId(indexingJob.targetPost() == null ? null : indexingJob.targetPost().id())
                             .build();
    }
}
