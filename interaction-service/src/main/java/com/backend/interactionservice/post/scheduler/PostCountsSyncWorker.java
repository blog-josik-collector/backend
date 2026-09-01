package com.backend.interactionservice.post.scheduler;

import com.backend.interactionservice.post.service.PostCountsSyncService;
import com.backend.interactionservice.post.service.dto.PostCountSyncResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * posts 테이블의 카운트 필드를 Elasticsearch techblog-posts 인덱스에 주기적으로 bulk upsert 하는 worker. <br>
 * PostViewCountFlushWorker 가 Redis view 를 DB 로 flush 한 뒤, 이 worker 가 DB 의 최신 카운트를 ES 로 반영하는 흐름을 기대한다. <br>
 * ES write alias 가 없으면 sync 를 건너뛴다({@link PostCountsSyncService}).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostCountsSyncWorker {

    private final PostCountsSyncService postCountsSyncService;

    @Scheduled(fixedDelayString = "${post-count-sync-worker.schedule-delay:30000}",
               initialDelayString = "${post-count-sync-worker.initial-delay:30000}")
    public void sync() {
        try {
            PostCountSyncResult result = postCountsSyncService.syncAll();
            if (result.totalPosts() == 0) {
                return;
            }

            if (result.failedCount() > 0) {
                log.warn("[PostCount] ES sync partial failure totalPosts={} successCount={} failedCount={}",
                         result.totalPosts(),
                         result.successCount(),
                         result.failedCount());
                return;
            }

            log.debug("[PostCount] ES sync completed totalPosts={} successCount={}",
                      result.totalPosts(),
                      result.successCount());
        } catch (Exception e) {
            log.error("[PostCount][IE50001] ES sync failed", e);
        }
    }
}
