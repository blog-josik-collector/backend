package com.backend.interactionservice.post.scheduler;

import com.backend.interactionservice.post.service.PostViewCountFlushService;
import com.backend.interactionservice.post.service.dto.PostViewCountFlushResult;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis 에 누적된 view 카운트를 주기적으로 PostgreSQL 로 flush 하는 worker. <br>
 * @Scheduled 진입점만 담당하며, 실제 drain → DB UPDATE → ack 흐름은 PostViewCountFlushService 에 위임한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostViewCountFlushWorker {

    private final PostViewCountFlushService postViewCountFlushService;

    @Scheduled(fixedDelayString = "${post-view-flusher.schedule-delay:30000}",
               initialDelayString = "${post-view-flusher.initial-delay:30000}")
    public void flush() {
        try {
            Optional<PostViewCountFlushResult> result = postViewCountFlushService.flushPendingToDb();
            result.ifPresent(r -> log.debug("Flushed view counts. posts={}, totalIncrement={}",
                                           r.postCount(),
                                           r.totalIncrement()));
        } catch (Exception e) {
            log.error("Failed to flush view counts to DB", e);
        }
    }
}
