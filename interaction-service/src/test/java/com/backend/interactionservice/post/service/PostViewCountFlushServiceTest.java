package com.backend.interactionservice.post.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;

import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.repository.PostViewCountRedisRepository;
import com.backend.interactionservice.post.service.dto.PostViewCountFlushResult;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PostViewCountFlushService 테스트")
@ExtendWith(MockitoExtension.class)
class PostViewCountFlushServiceTest {

    @InjectMocks
    private PostViewCountFlushService postViewCountFlushService;

    @Mock
    private PostViewCountRedisRepository postViewCountRedisRepository;

    @Mock
    private PostQueryRepository postQueryRepository;

    @DisplayName("flushPendingToDb 테스트")
    @Nested
    class FlushPendingToDbTest {

        @Test
        void pending이_있으면_DB에_반영하고_ack한_뒤_결과를_반환한다() {
            // given
            Map<UUID, Long> snapshot = Map.of(UUID.randomUUID(), 3L, UUID.randomUUID(), 5L);
            Mockito.doReturn(snapshot).when(postViewCountRedisRepository).drainPending();

            // when
            Optional<PostViewCountFlushResult> result = postViewCountFlushService.flushPendingToDb();

            // then
            Assertions.assertThat(result).isPresent();
            Assertions.assertThat(result.get().postCount()).isEqualTo(2);
            Assertions.assertThat(result.get().totalIncrement()).isEqualTo(8L);

            InOrder inOrder = Mockito.inOrder(postViewCountRedisRepository, postQueryRepository);
            inOrder.verify(postViewCountRedisRepository).drainPending();
            inOrder.verify(postQueryRepository).applyViewCounts(snapshot);
            inOrder.verify(postViewCountRedisRepository).ackFlushed(snapshot);
        }

        @Test
        void pending이_없으면_empty를_반환한다() {
            // given
            Mockito.doReturn(Map.of()).when(postViewCountRedisRepository).drainPending();

            // when
            Optional<PostViewCountFlushResult> result = postViewCountFlushService.flushPendingToDb();

            // then
            Assertions.assertThat(result).isEmpty();
            Mockito.verify(postQueryRepository, Mockito.never()).applyViewCounts(anyMap());
            Mockito.verify(postViewCountRedisRepository, Mockito.never()).ackFlushed(any());
        }

        @Test
        void DB_반영에_실패하면_ack하지_않는다() {
            // given
            Map<UUID, Long> snapshot = Map.of(UUID.randomUUID(), 1L);
            Mockito.doReturn(snapshot).when(postViewCountRedisRepository).drainPending();
            Mockito.doThrow(new RuntimeException("DB down")).when(postQueryRepository).applyViewCounts(snapshot);

            // when & then
            Assertions.assertThatThrownBy(() -> postViewCountFlushService.flushPendingToDb())
                      .isInstanceOf(RuntimeException.class)
                      .hasMessageContaining("DB down");
            Mockito.verify(postViewCountRedisRepository, Mockito.never()).ackFlushed(any());
        }
    }
}
