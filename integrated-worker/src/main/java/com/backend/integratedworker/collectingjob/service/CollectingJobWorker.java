package com.backend.integratedworker.collectingjob.service;

import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CollectingJobWorker {

    private final int jobBatchSize;
    private final CollectingJobPicker collectingJobPicker;
    private final CollectingJobExecutor collectingJobExecutor;

    public CollectingJobWorker(@Value("${collecting-job-worker.job-batch-size}") int jobBatchSize,
                               CollectingJobPicker collectingJobPicker,
                               CollectingJobExecutor collectingJobExecutor) {

        this.jobBatchSize = jobBatchSize;
        this.collectingJobPicker = collectingJobPicker;
        this.collectingJobExecutor = collectingJobExecutor;
    }

    @Scheduled(fixedDelayString = "${collecting-job-worker.schedule-delay}")
    public void poll() {
        List<UUID> pickedJobIds = collectingJobPicker.pickAndMarkRunning(jobBatchSize);
        if (!pickedJobIds.isEmpty()) {
            log.debug("[CollectingJob] jobs picked count={}", pickedJobIds.size());
        }
        for (UUID jobId : pickedJobIds) {
            collectingJobExecutor.executeAsync(jobId);
        }
    }
}
