package com.backend.interactionservice.post.scheduler;

import com.backend.interactionservice.post.service.PostCountsSyncService;
import com.backend.interactionservice.post.service.dto.PostCountSyncResult;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PostCountsSyncWorker 테스트")
@ExtendWith(MockitoExtension.class)
class PostCountsSyncWorkerTest {

    @InjectMocks
    private PostCountsSyncWorker postCountsSyncWorker;

    @Mock
    private PostCountsSyncService postCountsSyncService;

    @DisplayName("sync 테스트")
    @Nested
    class SyncTest {

        @Test
        void 동기화할_post가_없으면_조용히_종료한다() {
            Mockito.doReturn(PostCountSyncResult.skipped()).when(postCountsSyncService).syncAll();

            Assertions.assertThatCode(() -> postCountsSyncWorker.sync())
                      .doesNotThrowAnyException();

            Mockito.verify(postCountsSyncService).syncAll();
        }

        @Test
        void 모든_post가_성공하면_정상_종료한다() {
            Mockito.doReturn(new PostCountSyncResult(10, 10, 0)).when(postCountsSyncService).syncAll();

            Assertions.assertThatCode(() -> postCountsSyncWorker.sync())
                      .doesNotThrowAnyException();

            Mockito.verify(postCountsSyncService).syncAll();
        }

        @Test
        void 일부_post가_실패해도_정상_종료한다() {
            Mockito.doReturn(new PostCountSyncResult(10, 8, 2)).when(postCountsSyncService).syncAll();

            Assertions.assertThatCode(() -> postCountsSyncWorker.sync())
                      .doesNotThrowAnyException();

            Mockito.verify(postCountsSyncService).syncAll();
        }

        @Test
        void syncAll에서_예외가_발생해도_밖으로_던지지_않는다() {
            Mockito.doThrow(new RuntimeException("ES down")).when(postCountsSyncService).syncAll();

            // 스케줄러가 죽지 않도록 예외는 catch 되어야 함
            Assertions.assertThatCode(() -> postCountsSyncWorker.sync())
                      .doesNotThrowAnyException();
        }
    }
}
