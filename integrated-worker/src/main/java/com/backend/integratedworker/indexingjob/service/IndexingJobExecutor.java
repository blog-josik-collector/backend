package com.backend.integratedworker.indexingjob.service;

import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedworker.indexingjob.repository.IndexingJobQueryRepository;
import com.backend.integratedworker.indexingjob.service.dto.IndexingResult;
import com.backend.integratedworker.indexingjob.service.validator.IndexingJobValidator;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 비동기 실제 처리
 */
@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class IndexingJobExecutor {

    private final IndexingJobQueryRepository queryRepository;
    private final IndexingService indexingService;

    @Async("indexingExecutor")
    public void executeAsync(UUID jobId) {
        try {
            doIndexing(jobId);
            markSuccess(jobId);
        } catch (Exception e) {
            log.error("Indexing job failed: jobId={}", jobId, e);
            markFailed(jobId, e);
        }
    }

    protected void doIndexing(UUID jobId) {
        IndexingJob job = IndexingJobValidator.getIndexingJobOrThrow(jobId, queryRepository::fetchOneById);

        IndexingResult result = indexingService.executeIndexing(job);
        job.updateCounts(result.totalCount(), result.indexedCount());
    }

    protected void markSuccess(UUID jobId) {
        IndexingJob job = IndexingJobValidator.getIndexingJobOrThrow(jobId, queryRepository::fetchOneById);
        job.markSuccess(OffsetDateTime.now());
    }

    protected void markFailed(UUID jobId, Exception e) {
        IndexingJob job = IndexingJobValidator.getIndexingJobOrThrow(jobId, queryRepository::fetchOneById);
        job.markFailed(OffsetDateTime.now(), e.getMessage());
    }
}
