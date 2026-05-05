package com.backend.integratedworker.indexingjob.service;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * @Scheduled 진입점으로 두 가지 일을 합니다. <br> (a) 운영자가 만든 PENDING IndexingJob(MANUAL) 픽업 <br> (b) 글로벌 큐 드레인: PENDING CollectSourcePost가 있으면 CRON IndexingJob 생성
 */
@Slf4j
@Service
public class IndexingJobWorker {

    private final int jobBatchSize;
    private final int postBatchSize;

    private final IndexingJobPicker indexingJobPicker;
    private final IndexingJobExecutor indexingJobExecutor;

    public IndexingJobWorker(@Value("${indexing-job-worker.job-batch-size}") int jobBatchSize,
                             @Value("${indexing-job-worker.post-batch-size}") int postBatchSize,
                             IndexingJobPicker indexingJobPicker,
                             IndexingJobExecutor indexingJobExecutor) {

        this.jobBatchSize = jobBatchSize;
        this.postBatchSize = postBatchSize;
        this.indexingJobPicker = indexingJobPicker;
        this.indexingJobExecutor = indexingJobExecutor;
    }

    @Scheduled(fixedDelayString = "${indexing-job-worker.schedule-delay}")
    public void poll() {
        log.info("IndexingJobWorker call poll()");

        try {
            // (a) 운영자가 만든 PENDING IndexingJob(MANUAL) 픽업(API에서 만든 MANUAL)
            List<UUID> pendingJobIds = indexingJobPicker.pickPendingJobs(jobBatchSize);

            for (UUID jobId : pendingJobIds) {
                indexingJobExecutor.executeAsync(jobId);
            }

            // (b) 글로벌 큐 드레인: indexingStatus == PENDING인 CollectSourcePost가 있으면 CRON IndexingJob 생성 + CRON IndexingJob은 바로 RUNNING status에서 시작
            UUID cronJobId = indexingJobPicker.tryStartCronJob(postBatchSize);

            if (cronJobId != null) {
                indexingJobExecutor.executeAsync(cronJobId);
            }

        } catch (Exception e) {
            log.error("IndexingJobWorker poll failed", e);
        }
    }
}
