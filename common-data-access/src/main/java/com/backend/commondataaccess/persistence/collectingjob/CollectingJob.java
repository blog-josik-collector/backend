package com.backend.commondataaccess.persistence.collectingjob;

import com.backend.commondataaccess.persistence.common.enums.CollectingStatus;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.BaseEntity;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.common.enums.TriggerType;
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
@Table(name = "collecting_jobs")
@Entity
public class CollectingJob extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collect_source_id", nullable = false)
    private CollectSource collectSource;

    @Enumerated(EnumType.STRING)
    private JobStatus jobStatus;

    @Enumerated(EnumType.STRING)
    private CollectingStatus collectingStatus;

    @Enumerated(EnumType.STRING)
    private TriggerType triggerType;

    private UUID triggeredBy;

    private int totalCount;

    private int collectedCount;

    private int attemptCount;

    private String errorMessage;

    private OffsetDateTime startedAt;

    private OffsetDateTime endedAt;

    /**
     * 상태 전이 메서드
     */
    public void markRunning(OffsetDateTime now) {
        if (this.jobStatus != JobStatus.PENDING) {
            throw new IllegalStateException("PENDING이 아닌 Job은 RUNNING으로 못 바꿈");
        }
        this.jobStatus = JobStatus.RUNNING;
        this.startedAt = now;
        this.attemptCount++;
    }

    public void markSuccess(OffsetDateTime now) {
        this.jobStatus = JobStatus.SUCCESS;
        this.collectingStatus = CollectingStatus.PARSED;
        this.endedAt = now;
    }

    public void markFailed(OffsetDateTime now, String errorMessage) {
        this.jobStatus = JobStatus.FAILED;
        this.collectingStatus = CollectingStatus.PARSE_FAILED;
        this.endedAt = now;
        this.errorMessage = errorMessage;
    }

    public void updateCounts(int total, int collected) {
        this.totalCount = total;
        this.collectedCount = collected;
    }
}
