package com.backend.integratedworker.indexingjob.service;

import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedworker.indexingjob.repository.IndexingJobQueryRepository;
import com.backend.integratedworker.indexingjob.service.validator.IndexingJobValidator;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * IndexingJob 엔티티의 상태 변경을 짧은 트랜잭션 단위로 처리한다.
 * <p>
 * 옵션 B 설계상 IndexingJobExecutor의 outer @Transactional이 제거되면서, dirty checking에
 * 의존하던 updateCounts / markSuccess / markFailed가 동작할 트랜잭션 경계가 사라졌다.
 * 이 서비스가 각 변경 작업마다 짧은 트랜잭션을 열어서 즉시 커밋한다.
 */
@Transactional
@Service
@RequiredArgsConstructor
public class IndexingJobService {

    private final IndexingJobQueryRepository queryRepository;

    /**
     * 오케스트레이터가 IndexingService에 넘길 IndexingJob 엔티티를 조회한다.
     * 반환 시점에 트랜잭션이 닫히므로 detached 상태로 넘어간다.
     * LAZY 필드(targetSource, targetPost)는 id() 접근만 안전하다.
     */
    @Transactional(readOnly = true)
    public IndexingJob getJob(UUID jobId) {
        return IndexingJobValidator.getIndexingJobOrThrow(jobId, queryRepository::fetchOneById);
    }

    public void updateCounts(UUID jobId, int totalCount, int indexedCount) {
        IndexingJob job = IndexingJobValidator.getIndexingJobOrThrow(jobId, queryRepository::fetchOneById);
        job.updateCounts(totalCount, indexedCount);
    }

    public void markSuccess(UUID jobId, OffsetDateTime now) {
        IndexingJob job = IndexingJobValidator.getIndexingJobOrThrow(jobId, queryRepository::fetchOneById);
        job.markSuccess(now);
    }

    /**
     * 카운트 갱신과 SUCCESS 마킹을 한 트랜잭션으로 처리한다.
     */
    public void completeSuccess(UUID jobId, int totalCount, int indexedCount, OffsetDateTime now) {
        IndexingJob job = IndexingJobValidator.getIndexingJobOrThrow(jobId, queryRepository::fetchOneById);
        job.updateCounts(totalCount, indexedCount);
        job.markSuccess(now);
    }

    public void markFailed(UUID jobId, OffsetDateTime now, String errorMessage) {
        IndexingJob job = IndexingJobValidator.getIndexingJobOrThrow(jobId, queryRepository::fetchOneById);
        job.markFailed(now, errorMessage);
    }
}
