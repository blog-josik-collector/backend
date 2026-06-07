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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class CollectingJobCronCreationWorker {

    private final CollectingJobRepository collectingJobRepository;
    private final CollectingJobQueryRepository queryRepository;
    private final CollectSourceService collectSourceService;

    @Value("${crawler.default-from-page}")
    private int defaultFromPage;

    @Value("${crawler.default-to-page}")
    private int defaultToPage;

    @Scheduled(fixedDelayString = "${collecting-job-cron-generator.schedule-delay}")
    public void generate() {
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

                int fromPage = resolveFromPage(collectSource);
                int toPage = resolveToPage(collectSource);

                collectingJobRepository.save(CollectingJob.builder()
                                                              .collectSource(collectSource)
                                                              .jobStatus(JobStatus.PENDING)
                                                              .fromPage(fromPage)
                                                              .toPage(toPage)
                                                              .triggeredBy(null)
                                                              .build());
                log.debug("[CollectingJob] cron job scheduled sourceId={}", collectSource.id());

            } catch (Exception e) {
                log.error("[CollectingJob][BE50001] cron job generation failed sourceId={}", collectSource.id(), e);
            }
        }
    }

    private int resolveFromPage(CollectSource collectSource) {
        return collectSource.cronFromPage() != null ? collectSource.cronFromPage() : defaultFromPage;
    }

    private int resolveToPage(CollectSource collectSource) {
        return collectSource.cronToPage() != null ? collectSource.cronToPage() : defaultToPage;
    }

    private boolean isCronDue(String expression, OffsetDateTime now) {
        var cron = CronExpression.parse(expression);
        var prev = now.minusMinutes(1);
        var next = cron.next(prev);
        return next != null && !next.isAfter(now);
    }
}
