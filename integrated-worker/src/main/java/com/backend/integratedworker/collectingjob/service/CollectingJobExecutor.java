package com.backend.integratedworker.collectingjob.service;

import com.backend.commondataaccess.exception.BusinessException;
import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.integratedworker.collectingjob.repository.CollectingJobQueryRepository;
import com.backend.integratedworker.collectingjob.service.crawler.BlogCrawlerService;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import com.backend.integratedworker.collectingjob.service.validator.CollectingJobValidator;
import com.backend.integratedworker.collectsourcepost.service.CollectSourcePostService;
import jakarta.transaction.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Executor: 실제 작업 (별도 트랜잭션 + 비동기)
 */
@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class CollectingJobExecutor {

    private final CollectingJobQueryRepository queryRepository;
    private final BlogCrawlerService blogCrawlerService;
    private final CollectSourcePostService collectSourcePostService;

    @Async("collectingExecutor")
    public void executeAsync(UUID jobId) {
        try {
            doCollect(jobId);
            markSuccess(jobId);
        } catch (Exception e) {
            markFailed(jobId, e);
        }
    }

    protected void doCollect(UUID jobId) {
        CollectingJob collectingJob = CollectingJobValidator.getCollectingJobOrThrow(jobId, queryRepository::fetchOneById);
        CollectSource source = collectingJob.collectSource();
        List<Post> posts = blogCrawlerService.fetch(collectingJob);
        for (Post post : posts) {
            CollectSourcePost collectSourcePost = collectSourcePostService.getCollectSourcePost(post.getUrl());

            if (collectSourcePost == null) {
                collectSourcePostService.create(post, source, collectingJob);
                continue;
            }

            if (collectingJob.forceRecollect()) {
                collectSourcePostService.update(collectSourcePost.id(), post, collectingJob);
                continue;
            }

            String contentHash = collectSourcePostService.createContentHash(post);

            if (StringUtils.equals(contentHash, collectSourcePost.contentHash())) {
                collectSourcePost.updateLastCollect(collectingJob, OffsetDateTime.now());
            } else {
                collectSourcePostService.update(collectSourcePost.id(), post, collectingJob);
            }
        }

        collectingJob.updateCounts(posts.size(), posts.size());
    }

    protected void markSuccess(UUID jobId) {
        CollectingJob collectingJob = CollectingJobValidator.getCollectingJobOrThrow(jobId, queryRepository::fetchOneById);
        collectingJob.markSuccess(OffsetDateTime.now());
    }

    protected void markFailed(UUID jobId, Exception e) {
        if (e instanceof BusinessException businessException) {
            log.error("[CollectingJob][{}] job failed jobId={}",
                      businessException.getErrorCode().getCode(), jobId, e);
        } else {
            log.error("[CollectingJob][BE50001] job failed jobId={}", jobId, e);
        }
        CollectingJob collectingJob = CollectingJobValidator.getCollectingJobOrThrow(jobId, queryRepository::fetchOneById);
        collectingJob.markFailed(OffsetDateTime.now(), e.getMessage());
    }
}
