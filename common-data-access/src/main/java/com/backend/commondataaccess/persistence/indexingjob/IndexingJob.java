package com.backend.commondataaccess.persistence.indexingjob;

import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.common.BaseEntity;
import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "indexing_jobs")
@Entity
public class IndexingJob extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Enumerated(EnumType.STRING)
    private IndexingJobType indexingJobType;

    @Enumerated(EnumType.STRING)
    private JobStatus jobStatus;

    private int totalCount;

    private int indexedCount;

    private String errorMessage;

    private OffsetDateTime startedAt;

    private OffsetDateTime endedAt;

    private UUID triggeredBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_source_id")
    private CollectSource targetSource;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_post_id")
    private CollectSourcePost targetPost;

    public void markRunning(OffsetDateTime now) {
        if (this.jobStatus != JobStatus.PENDING) {
            throw new IllegalStateException("PENDING이 아닌 Job은 RUNNING으로 못 바꿈");
        }
        this.jobStatus = JobStatus.RUNNING;
        this.startedAt = now;
    }
    public void markSuccess(OffsetDateTime now) {
        this.jobStatus = JobStatus.SUCCESS;
        this.endedAt = now;
    }
    public void markFailed(OffsetDateTime now, String errorMessage) {
        this.jobStatus = JobStatus.FAILED;
        this.endedAt = now;
        this.errorMessage = errorMessage;
    }
    public void updateCounts(int total, int indexed) {
        this.totalCount = total;
        this.indexedCount = indexed;
    }
}
