package com.backend.integratedworker.collectingjob.service;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.integratedworker.collectingjob.repository.CollectingJobQueryRepository;
import com.backend.integratedworker.collectingjob.repository.CollectingJobRepository;
import com.backend.integratedworker.collectingjob.service.validator.CollectingJobValidator;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Executor: 실제 작업 (별도 트랜잭션 + 비동기)
 */
@Service
@RequiredArgsConstructor
public class CollectingJobExecutor {

    private final CollectingJobRepository collectingJobRepository;
    private final CollectingJobQueryRepository queryRepository;
//    private final CollectSourcePostRepository postRepository;

    @Async("collectingExecutor")
    public void executeAsync(UUID jobId) {
        try {
            doCollect(jobId);
            markSuccess(jobId);
        } catch (Exception e) {
            markFailed(jobId, e);
        }
    }

    @Transactional
    protected void doCollect(UUID jobId) {
        CollectingJob collectingJob = CollectingJobValidator.getCollectingJobOrThrow(jobId, queryRepository::fetchOneById);
        CollectSource source = collectingJob.collectSource();
//        var fetched = crawler.fetch(source.url());
//        for (var raw : fetched) {
//            postRepository.save(toEntity(raw, job));
//        }
//        job.updateCounts(fetched.size(), fetched.size());
    }

    @Transactional
    protected void markSuccess(UUID jobId) {
        CollectingJob collectingJob = CollectingJobValidator.getCollectingJobOrThrow(jobId, queryRepository::fetchOneById);
        collectingJob.markSuccess(OffsetDateTime.now());
    }

    @Transactional
    protected void markFailed(UUID jobId, Exception e) {
        CollectingJob collectingJob = CollectingJobValidator.getCollectingJobOrThrow(jobId, queryRepository::fetchOneById);
        collectingJob.markFailed(OffsetDateTime.now(), e.getMessage());
    }
}
