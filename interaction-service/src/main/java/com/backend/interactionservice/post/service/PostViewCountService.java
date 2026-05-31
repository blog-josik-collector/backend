package com.backend.interactionservice.post.service;

import com.backend.interactionservice.post.repository.PostViewCountRedisRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 포스팅 조회수 트래킹 service. 매 view 마다 Redis 의 누적 카운터를 1씩 올리며, DB 반영은 PostViewCountFlushService 가 주기적으로 일괄 처리한다.
 */
@Service
@RequiredArgsConstructor
public class PostViewCountService {

    private final PostViewCountRedisRepository postViewCountRedisRepository;

    /**
     * 한 번의 포스팅 조회를 기록한다. 호출자는 view 정의를 만족한 시점에만 호출해야 한다 (예: 단건 상세 조회 응답 직전, 게시글이 실제로 존재할 때).
     */
    public void recordView(UUID postId) {
        if (postId == null) {
            return;
        }
        postViewCountRedisRepository.increment(postId);
    }
}
