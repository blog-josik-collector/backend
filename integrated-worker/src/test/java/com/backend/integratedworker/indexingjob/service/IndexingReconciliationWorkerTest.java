package com.backend.integratedworker.indexingjob.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import com.backend.integratedworker.collectsourcepost.service.CollectSourcePostService;
import java.time.OffsetDateTime;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("IndexingReconciler 테스트")
@ExtendWith(MockitoExtension.class)
class IndexingReconciliationWorkerTest {

    private static final int STALE_THRESHOLD_MINUTES = 15;
    private static final int BATCH_SIZE = 100;

    @Mock
    private CollectSourcePostService collectSourcePostService;

    private IndexingReconciliationWorker indexingReconciliationWorker;

    @BeforeEach
    void init() {
        indexingReconciliationWorker = new IndexingReconciliationWorker(STALE_THRESHOLD_MINUTES, BATCH_SIZE, collectSourcePostService);
    }

    @DisplayName("reconcile 테스트")
    @Nested
    class ReconcileTest {

        @Test
        void stale_INDEXING_post가_있으면_recoverStaleIndexing이_호출된다() {
            Mockito.doReturn(3).when(collectSourcePostService).recoverStaleIndexing(any(OffsetDateTime.class), anyInt());

            indexingReconciliationWorker.reconcile();

            Mockito.verify(collectSourcePostService).recoverStaleIndexing(any(OffsetDateTime.class), eq(BATCH_SIZE));
        }

        @Test
        void threshold는_현재시각으로부터_설정된_분만큼_이전으로_전달된다() {
            Mockito.doReturn(0).when(collectSourcePostService).recoverStaleIndexing(any(OffsetDateTime.class), anyInt());

            OffsetDateTime before = OffsetDateTime.now();
            indexingReconciliationWorker.reconcile();
            OffsetDateTime after = OffsetDateTime.now();

            ArgumentCaptor<OffsetDateTime> thresholdCaptor = ArgumentCaptor.forClass(OffsetDateTime.class);
            Mockito.verify(collectSourcePostService).recoverStaleIndexing(thresholdCaptor.capture(), eq(BATCH_SIZE));

            OffsetDateTime threshold = thresholdCaptor.getValue();
            Assertions.assertThat(threshold).isAfterOrEqualTo(before.minusMinutes(STALE_THRESHOLD_MINUTES));
            Assertions.assertThat(threshold).isBeforeOrEqualTo(after.minusMinutes(STALE_THRESHOLD_MINUTES));
        }

        @Test
        void 복구된_건이_없으면_조용히_종료한다() {
            Mockito.doReturn(0).when(collectSourcePostService).recoverStaleIndexing(any(OffsetDateTime.class), anyInt());

            // 예외 없이 정상 종료해야 함
            Assertions.assertThatCode(() -> indexingReconciliationWorker.reconcile())
                      .doesNotThrowAnyException();
        }

        @Test
        void recoverStaleIndexing에서_예외가_발생해도_밖으로_던지지_않는다() {
            Mockito.doThrow(new RuntimeException("DB down"))
                   .when(collectSourcePostService)
                   .recoverStaleIndexing(any(OffsetDateTime.class), anyInt());

            // 스케줄러가 죽지 않도록 예외는 catch 되어야 함
            Assertions.assertThatCode(() -> indexingReconciliationWorker.reconcile())
                      .doesNotThrowAnyException();
        }
    }
}
