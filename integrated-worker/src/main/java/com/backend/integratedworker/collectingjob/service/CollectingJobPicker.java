package com.backend.integratedworker.collectingjob.service;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.integratedworker.collectingjob.repository.CollectingJobRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Picker: 트랜잭션 1 (짧은 픽업 트랜잭션) pickPending 결과를 같은 트랜잭션 안에서 RUNNING으로 업데이트하고 끝. </br> 여기서 lock 풀림.
 */
@Transactional
@Service
@RequiredArgsConstructor
public class CollectingJobPicker {

    private final CollectingJobRepository collectingJobRepository;

    public List<UUID> pickAndMarkRunning(int batchSize) {
        List<CollectingJob> jobs = collectingJobRepository.pickPending(batchSize);
        OffsetDateTime now = OffsetDateTime.now();

        for (CollectingJob job : jobs) {
            job.markRunning(now);
        }

        return jobs.stream().map(CollectingJob::id).toList();
    }
}
