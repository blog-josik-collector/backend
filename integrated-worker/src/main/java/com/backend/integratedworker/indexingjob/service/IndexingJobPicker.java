package com.backend.integratedworker.indexingjob.service;

import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedworker.collectsourcepost.service.CollectSourcePostService;
import com.backend.integratedworker.indexingjob.repository.IndexingJobRepository;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 짧은 트랜잭션 (락 + 상태 전이)
 *
 * CRON Job은 RUNNING으로 곧장 INSERT: 어차피 워커 자신이 picker → executor로 이어서 처리하므로, PENDING으로 만들 필요가 없음. (MANUAL은 API에서 PENDING으로 만들고 워커가 픽업) <br>
 * post의 INDEXING 마킹과 Job 생성이 같은 트랜잭션 → race-free <br>
 * FOR UPDATE SKIP LOCKED로 멀티 워커 인스턴스 안전
 */
@Transactional
@Service
@RequiredArgsConstructor
public class IndexingJobPicker {

    private final IndexingJobRepository indexingJobRepository;
    private final CollectSourcePostService collectSourcePostService;

    /**
     * (a) MANUAL/PENDING 인 IndexingJob을 픽업해서 RUNNING으로 마킹.
     */
    public List<UUID> pickPendingJobs(int batchSize) {
        List<IndexingJob> jobs = indexingJobRepository.findAllPendingIndexingJobs(batchSize);
        OffsetDateTime now = OffsetDateTime.now();
        for (IndexingJob job : jobs) {
            job.markRunning(now);
        }
        return jobs.stream().map(IndexingJob::id).toList();
    }

    /**
     * (b) PENDING post가 있으면 새 CRON IndexingJob 생성 + 해당 post들을 INDEXING으로 마킹. post들이 lastIndexingJob을 통해 새 Job에 묶임.
     *
     * @return 생성된 jobId, 없으면 null
     */
    public UUID tryStartCronJob(int postBatchSize) {
        List<CollectSourcePost> pendingPosts = collectSourcePostService.pickPendingForIndexing(postBatchSize);

        if (pendingPosts.isEmpty()) {
            return null;
        }

        OffsetDateTime now = OffsetDateTime.now();
        IndexingJob job = IndexingJob.builder()
                                     .indexingJobType(IndexingJobType.CRON)
                                     .jobStatus(JobStatus.RUNNING)
                                     .startedAt(now)
                                     .totalCount(pendingPosts.size())
                                     .indexedCount(0)
                                     .build();

        IndexingJob saved = indexingJobRepository.save(job);

        for (CollectSourcePost p : pendingPosts) {
            p.markIndexing(saved);
        }

        return saved.id();
    }
}
