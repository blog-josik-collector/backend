package com.backend.integratedworker.indexingjob.service;

import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedworker.collectsourcepost.service.CollectSourcePostService;
import com.backend.integratedworker.common.service.elasticsearch.ElasticsearchService;
import com.backend.integratedworker.common.service.elasticsearch.dto.BulkIndexResult;
import com.backend.integratedworker.common.service.elasticsearch.dto.EsPostDocument;
import com.backend.integratedworker.indexingjob.service.dto.IndexingResult;
import com.backend.integratedworker.post.service.PostService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 도메인 로직 <br>
 * <p>
 * CRON과 MANUAL의 처리 차이를 흡수하는 곳
 */
@Service
@RequiredArgsConstructor
public class IndexingService {

    private final ElasticsearchService elasticsearchService;
    private final CollectSourcePostService collectSourcePostService;
    private final PostService postService;

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
        List<CollectSourcePost> collectSourcePosts = collectSourcePostService.markIndexingBatch(job);

        return bulkIndexAndApply(job, collectSourcePosts);
    }

    private IndexingResult bulkIndexAndApply(IndexingJob job, List<CollectSourcePost> targets) {
        if (targets.isEmpty()) {
            return new IndexingResult(0, 0);
        }

        List<UUID> targetIds = targets.stream()
                                 .map(CollectSourcePost::id)
                                 .toList();

        List<EsPostDocument> documents = targets.stream()
                                                .map(EsPostDocument::from)
                                                .toList();

        // TX1
        postService.createPostsIfAbsent(targetIds);

        // Out of TX
        BulkIndexResult bulkResult = elasticsearchService.bulkIndex(documents);

        // TX2
        collectSourcePostService.applyIndexResult(targetIds, bulkResult, job, OffsetDateTime.now());

        return new IndexingResult(targets.size(), bulkResult.getSuccessCount());
    }
}
