package com.backend.integratedworker.collectingjob.service;

import com.backend.commondataaccess.exception.BusinessException;
import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.integratedworker.collectingjob.service.crawler.BlogCrawlerService;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 비동기 실제 처리 오케스트레이터.
 * <p>
 * 크롤링(I/O)은 TX 밖, post persist + job SUCCESS는 {@link CollectingJobService#finishCollect} 한 TX.
 * Picker는 jobId만 넘기고, 여기서 ID로 재조회한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectingJobExecutor {

    private final CollectingJobService collectingJobService;
    private final BlogCrawlerService blogCrawlerService;

    @Async("collectingExecutor")
    public void executeAsync(UUID jobId) {
        try {
            CollectingJob collectingJob = collectingJobService.getJobForExecution(jobId);
            List<Post> posts = blogCrawlerService.fetch(collectingJob);
            collectingJobService.finishCollect(
                    jobId,
                    collectingJob.collectSource().id(),
                    collectingJob.forceRecollect(),
                    posts,
                    OffsetDateTime.now());
        } catch (Exception e) {
            if (e instanceof BusinessException businessException) {
                log.error("[CollectingJob][{}] job failed jobId={}",
                          businessException.getErrorCode().getCode(), jobId, e);
            } else {
                log.error("[CollectingJob][BE50001] job failed jobId={}", jobId, e);
            }
            collectingJobService.markFailed(jobId, OffsetDateTime.now(), e.getMessage());
        }
    }
}
