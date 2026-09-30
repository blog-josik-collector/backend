package com.backend.interactionservice.post.scheduler;

import com.backend.interactionservice.post.service.PostViewCountFlushService;
import com.backend.interactionservice.post.service.dto.PostViewCountFlushResult;
import java.util.Optional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PostViewCountFlushWorker 테스트")
@ExtendWith(MockitoExtension.class)
class PostViewCountFlushWorkerTest {

    @InjectMocks
    private PostViewCountFlushWorker postViewCountFlushWorker;

    @Mock
    private PostViewCountFlushService postViewCountFlushService;

    @DisplayName("flush 테스트")
    @Nested
    class FlushTest {

        @Test
        void flush_결과가_있으면_정상_종료한다() {
            Mockito.doReturn(Optional.of(PostViewCountFlushResult.of(3, 10L))).when(postViewCountFlushService).flushPendingToDb();

            Assertions.assertThatCode(() -> postViewCountFlushWorker.flush())
                      .doesNotThrowAnyException();

            Mockito.verify(postViewCountFlushService).flushPendingToDb();
        }

        @Test
        void flush_결과가_없으면_조용히_종료한다() {
            Mockito.doReturn(Optional.empty()).when(postViewCountFlushService).flushPendingToDb();

            Assertions.assertThatCode(() -> postViewCountFlushWorker.flush())
                      .doesNotThrowAnyException();

            Mockito.verify(postViewCountFlushService).flushPendingToDb();
        }

        @Test
        void flushPendingToDb에서_예외가_발생해도_밖으로_던지지_않는다() {
            Mockito.doThrow(new RuntimeException("DB down")).when(postViewCountFlushService).flushPendingToDb();

            // 스케줄러가 죽지 않도록 예외는 catch 되어야 함
            Assertions.assertThatCode(() -> postViewCountFlushWorker.flush())
                      .doesNotThrowAnyException();
        }
    }
}
