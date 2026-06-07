package com.backend.interactionservice.post.service;

import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commonelasticsearch.operation.bulk.BulkOperationResult;
import com.backend.interactionservice.post.repository.PostCountsElasticsearchRepository;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.service.dto.PostCountSyncResult;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * posts 테이블의 카운트(like/view/comment/report)를 Elasticsearch 로 동기화하는 domain service. <br>
 * soft-delete 되지 않은 모든 post 를 id keyset pagination 으로 batch 조회한 뒤 ES bulk partial upsert 를 반복 호출한다.
 * <p>
 * DB 조회와 ES bulk 를 분리해 read-only 트랜잭션이 ES I/O 동안 커넥션을 점유하지 않도록 한다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class PostCountsSyncService {

    private final PostQueryRepository postQueryRepository;
    private final PostCountsElasticsearchRepository postCountsElasticsearchRepository;

    @Value("${post-count-sync-worker.post-batch-size:100}")
    private int postBatchSize;

    public PostCountSyncResult syncAll() {
        UUID cursor = null;
        int totalPosts = 0;
        int successCount = 0;
        int failedCount = 0;

        while (true) {
            List<Post> batch = fetchActivePostsBatch(cursor);
            if (batch.isEmpty()) {
                break;
            }

            BulkOperationResult result = postCountsElasticsearchRepository.bulkUpsertCounts(batch);
            totalPosts += batch.size();
            successCount += result.successCount();
            failedCount += result.failedIds().size();
            cursor = batch.get(batch.size() - 1).id();
        }

        return new PostCountSyncResult(totalPosts, successCount, failedCount);
    }

    @Transactional(readOnly = true)
    protected List<Post> fetchActivePostsBatch(UUID cursor) {
        return postQueryRepository.fetchActivePostsAfterId(cursor, postBatchSize);
    }
}
