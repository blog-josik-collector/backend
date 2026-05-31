package com.backend.interactionservice.post.service;

import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.repository.PostViewCountRedisRepository;
import com.backend.interactionservice.post.service.dto.PostViewCountFlushResult;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Redis 에 누적된 view 카운트를 PostgreSQL posts.view_count 로 flush 하는 domain service. <br>
 * 처리 순서: drain (snapshot) → DB UPDATE → Redis 차감 (ack). <br>
 * 기존 PostViewCountFlushWorker 와 동일하게 이 메서드 전체에 @Transactional 을 걸어 drain → applyViewCounts → ack 순서와 실패 시 ack 생략 동작을 유지한다.
 */
@Service
@RequiredArgsConstructor
public class PostViewCountFlushService {

    private final PostViewCountRedisRepository postViewCountRedisRepository;
    private final PostQueryRepository postQueryRepository;

    /**
     * pending view 카운트를 DB 로 반영한다. pending 이 없으면 empty 를 반환한다. <br>
     * applyViewCounts 가 실패하면 트랜잭션이 rollback 되고 ack 도 호출되지 않는다.
     */
    @Transactional
    public Optional<PostViewCountFlushResult> flushPendingToDb() {
        Map<UUID, Long> snapshot = postViewCountRedisRepository.drainPending();
        if (snapshot.isEmpty()) {
            return Optional.empty();
        }

        postQueryRepository.applyViewCounts(snapshot);
        postViewCountRedisRepository.ackFlushed(snapshot);

        long totalIncrement = snapshot.values().stream().mapToLong(Long::longValue).sum();
        return Optional.of(PostViewCountFlushResult.of(snapshot.size(), totalIncrement));
    }
}
