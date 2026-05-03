package com.backend.integratedworker.collectingjob.service;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.integratedworker.collectingjob.repository.CollectingJobQueryRepository;
import com.backend.integratedworker.collectingjob.repository.CollectingJobRepository;
import com.backend.integratedworker.collectsource.service.CollectSourceService;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class CollectingJobCronGenerator {

    private final CollectingJobRepository collectingJobRepository;
    private final CollectingJobQueryRepository queryRepository;
    private final CollectSourceService collectSourceService;

    //    @Scheduled(fixedDelay = 5000)
    @Scheduled(fixedDelayString = "${scheduler.cron-job-generator.delay}")
    public void generate() {
        log.info("CollectingJob Cron Generator call generate()");

        OffsetDateTime now = OffsetDateTime.now();
        List<CollectSource> collectSources = collectSourceService.getActiveCronCollectSources();
        for (CollectSource collectSource : collectSources) {
            try {
                if (!isCronDue(collectSource.cronExpression(), now)) {
                    continue;
                }
                if (queryRepository.existsActiveJob(collectSource.id())) {
                    continue;
                }

                collectingJobRepository.save(CollectingJob.builder()
                                                          .collectSource(collectSource)
                                                          .jobStatus(JobStatus.PENDING)
                                                          .triggeredBy(null)
                                                          .build());

            } catch (Exception e) {
                log.error("CronGenerator failed for source {}", collectSource.id(), e);
            }
        }
    }

    private boolean isCronDue(String expression, OffsetDateTime now) {
        var cron = CronExpression.parse(expression);
        var prev = now.minusMinutes(1);
        var next = cron.next(prev);
        return next != null && !next.isAfter(now);
    }
}
