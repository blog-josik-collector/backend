package com.backend.integratedworker.indexingjob.service;

import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedworker.collectsourcepost.service.CollectSourcePostService;
import com.backend.integratedworker.common.service.elasticsearch.ElasticsearchService;
import com.backend.integratedworker.common.service.elasticsearch.dto.BulkIndexResult;
import com.backend.integratedworker.common.service.elasticsearch.dto.EsPostDocument;
import com.backend.integratedworker.indexingjob.service.dto.IndexingResult;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도메인 로직 <br>
 * <p>
 * CRON과 MANUAL의 처리 차이를 흡수하는 곳
 */
@Transactional
@Service
@RequiredArgsConstructor
public class IndexingService {

    private final ElasticsearchService elasticsearchService;
    private final CollectSourcePostService collectSourcePostService;

    @Transactional
    public IndexingResult executeIndexing(IndexingJob job) {
        return switch (job.indexingJobType()) {
            case CRON -> indexingCronJob(job);
            case MANUAL -> indexingManualJob(job);
        };
    }

    /**
     * Picker가 이미 INDEXING으로 마킹해놓은 post들을 ES bulk index.
     */
    private IndexingResult indexingCronJob(IndexingJob job) {
        List<CollectSourcePost> indexingCollectSourcePosts = collectSourcePostService.getIndexingCollectSourcePosts(job.id());
        return bulkIndexAndApply(job, indexingCollectSourcePosts);
    }


    /**
     * MANUAL 재색인: target source의 모든 후보 post를 INDEXING으로 마킹 후 ES bulk index.
     */
    private IndexingResult indexingManualJob(IndexingJob job) {
        List<CollectSourcePost> targets = resolveManualTargets(job);

        for (var p : targets) {
            p.markIndexing(job);
        }
        return bulkIndexAndApply(job, targets);
    }

    private List<CollectSourcePost> resolveManualTargets(IndexingJob job) {
        if (job.targetSource() != null && job.targetPost() == null) {
            // collectSource 전체 재색인
            return collectSourcePostService.getReindexTargetCollectSourcePosts(job.targetSource().id());
        }

        if (job.targetSource() == null && job.targetPost() != null) {
            // 단일 collectSourcePost 재색인
            return List.of(job.targetPost());
        }

        throw new IllegalStateException(
                "MANUAL job 대상이 잘못됨: jobId=" + job.id()
                        + ", targetSource=" + job.targetSource()
                        + ", targetPost=" + job.targetPost()
        );
    }

    private IndexingResult bulkIndexAndApply(IndexingJob job, List<CollectSourcePost> targets) {
        if (targets.isEmpty()) {
            return new IndexingResult(0, 0);
        }

        List<EsPostDocument> documents = targets.stream()
                                                .map(EsPostDocument::from)
                                                .toList();

        BulkIndexResult bulkResult = elasticsearchService.bulkIndex(documents);
        applyBulkResult(targets, bulkResult, job);
        return new IndexingResult(targets.size(), bulkResult.getSuccessCount());
    }

    private void applyBulkResult(List<CollectSourcePost> posts, BulkIndexResult result, IndexingJob job) {
        OffsetDateTime now = OffsetDateTime.now();
        for (CollectSourcePost p : posts) {
            if (result.isFailed(p.id())) {
                p.markIndexFailed(job);
            } else {
                p.markIndexed(job, now);
            }
        }
    }
}
