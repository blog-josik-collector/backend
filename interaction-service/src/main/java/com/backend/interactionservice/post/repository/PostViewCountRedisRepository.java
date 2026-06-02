package com.backend.interactionservice.post.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 포스팅 조회수를 Redis Hash 에 누적하는 repository. <br>
 * - 키 구조: post_view:pending = { postId(String) -> count(Long) } <br>
 * - increment 는 HINCRBY 한 번 호출로 끝나기 때문에 web 요청 latency 에 거의 영향이 없다. <br>
 * - flush 는 PostViewCountFlushService 가 주기적으로 drainPending() → ackFlushed() 순으로 호출한다.
 */
@Slf4j
@Repository
public class PostViewCountRedisRepository {

    private final String redisHashKey;
    private final StringRedisTemplate redis;

    public PostViewCountRedisRepository(@Value("${spring.data.redis.hash-key}") String redisHashKey,
                                        StringRedisTemplate redis) {

        this.redisHashKey = redisHashKey;
        this.redis = redis;
    }

    /**
     * 단일 포스팅의 view 카운트를 1 증가시킨다. 매 view 마다 호출되는 hot path.
     */
    public void increment(UUID postId) {
        try {
            redis.opsForHash().increment(redisHashKey, postId.toString(), 1L);
        } catch (Exception e) {
            // Redis 장애가 user-facing 응답을 깨뜨리지 않도록 view 카운트는 best-effort로 처리한다. 손실된 view 는 다음 정상 요청부터 다시 누적된다.
            log.warn("[PostViewCount][IE50002] failed to buffer view postId={}", postId, e);
        }
    }

    /**
     * 현재 시점의 pending snapshot을 (postId -> count) 맵으로 반환한다. 이 시점 이후에 들어오는 view 는 다음 flush 사이클에 처리된다. 키가 없거나 비어있으면 빈 맵을 반환한다.
     */
    public Map<UUID, Long> drainPending() {
        Map<Object, Object> entries = redis.opsForHash().entries(redisHashKey);
        if (entries.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Long> result = new HashMap<>(entries.size());
        for (Map.Entry<Object, Object> e : entries.entrySet()) {
            try {
                UUID postId = UUID.fromString(e.getKey().toString());
                long count = Long.parseLong(e.getValue().toString());
                if (count > 0) {
                    result.put(postId, count);
                }
            } catch (RuntimeException parseError) {
                log.warn("[PostViewCount] skipping invalid buffer entry key={} value={}", e.getKey(), e.getValue());
            }
        }
        return result;
    }

    /**
     * snapshot 으로 받은 분량만큼 Redis 카운터에서 차감한다. 차감 사이에 들어온 새 view 는 다음 사이클에 자연스럽게 누적되어 손실되지 않는다.
     */
    public void ackFlushed(Map<UUID, Long> snapshot) {
        if (snapshot.isEmpty()) {
            return;
        }
        HashOperations<String, Object, Object> ops = redis.opsForHash();
        for (Map.Entry<UUID, Long> e : snapshot.entrySet()) {
            try {
                ops.increment(redisHashKey, e.getKey().toString(), -e.getValue());
            } catch (Exception ex) {
                log.warn("[PostViewCount][IE50002] failed to ack flushed view postId={} count={}", e.getKey(), e.getValue(), ex);
            }
        }
    }
}
