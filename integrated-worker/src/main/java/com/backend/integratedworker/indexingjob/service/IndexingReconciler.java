package com.backend.integratedworker.indexingjob.service;

import com.backend.integratedworker.collectsourcepost.service.CollectSourcePostService;
import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * 색인 정합성 안전망.
 * <p>
 * IndexingService의 흐름은 [TX1: posts insert] → [ES bulkIndex] → [TX2: collect_source_posts 마킹] 으로 쪼개져 있어서, TX1 커밋 후 ~ TX2 커밋 전 사이에 프로세스가 죽으면 collect_source_posts가 INDEXING 상태로 영원히 갇힐 수 있다.
 * <p>
 * 이 컴포넌트는 일정 시간(staleThreshold) 이상 INDEXING 상태로 머물러 있는 post들을 PENDING으로 되돌려 다음 Picker가 다시 처리하게 한다. ES는 같은 _id에 대해 upsert(멱등), posts insert는 createPostsIfAbsent(멱등) 이므로 재처리해도 안전하다.
 */
@Slf4j
@Service
public class IndexingReconciler {

    private final int staleThresholdMinutes;
    private final int batchSize;
    private final CollectSourcePostService collectSourcePostService;

    public IndexingReconciler(@Value("${indexing-reconciler.stale-threshold-minutes}") int staleThresholdMinutes,
                              @Value("${indexing-reconciler.batch-size}") int batchSize,
                              CollectSourcePostService collectSourcePostService) {

        this.staleThresholdMinutes = staleThresholdMinutes;
        this.batchSize = batchSize;
        this.collectSourcePostService = collectSourcePostService;
    }

    @Scheduled(fixedDelayString = "${indexing-reconciler.schedule-delay}")
    public void reconcile() {
        try {
            OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(staleThresholdMinutes);

            int recovered = collectSourcePostService.recoverStaleIndexing(threshold, batchSize);

            if (recovered > 0) {
                log.warn("[IndexingJob] stale INDEXING posts recovered count={} thresholdMinutes={} batchSize={}",
                         recovered, staleThresholdMinutes, batchSize);
            }
        } catch (Exception e) {
            log.error("[IndexingJob][BE50001] reconcile failed", e);
        }
    }
}
