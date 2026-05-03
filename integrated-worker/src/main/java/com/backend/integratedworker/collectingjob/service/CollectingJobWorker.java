package com.backend.integratedworker.collectingjob.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CollectingJobWorker {

    private final CollectingJobPicker picker;
    private final CollectingJobExecutor executor;

//    @Scheduled(fixedDelay = 5000)
    @Scheduled(fixedDelayString = "${custom.scheduler.delay}")
    public void poll() {
        log.info("Collecting job picker started");
        List<UUID> pickedJobIds = picker.pickAndMarkRunning(10);
        for (UUID jobId : pickedJobIds) {
            executor.executeAsync(jobId);
        }
    }
}
