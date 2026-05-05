package com.backend.integratedworker.indexingjob.service;

import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedworker.indexingjob.repository.IndexingJobQueryRepository;
import com.backend.integratedworker.indexingjob.service.dto.IndexingResult;
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
        IndexingJob job = queryRepository.fetchOneById(jobId)
                                         .orElseThrow(() -> new IllegalStateException("IndexingJob not found: " + jobId));

        IndexingResult result = indexingService.executeIndexing(job);
        job.updateCounts(result.totalCount(), result.indexedCount());
    }

    protected void markSuccess(UUID jobId) {
        queryRepository.fetchOneById(jobId)
                       .ifPresent(j -> j.markSuccess(OffsetDateTime.now()));
    }

    protected void markFailed(UUID jobId, Exception e) {
        queryRepository.fetchOneById(jobId)
                       .ifPresent(j -> j.markFailed(OffsetDateTime.now(), e.getMessage()));
    }
}
