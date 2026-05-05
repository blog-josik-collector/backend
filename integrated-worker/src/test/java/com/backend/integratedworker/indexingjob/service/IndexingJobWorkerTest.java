package com.backend.integratedworker.indexingjob.service;

import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("IndexingJobWorker 테스트")
@ExtendWith(MockitoExtension.class)
class IndexingJobWorkerTest {

    private static final int JOB_BATCH_SIZE = 5;
    private static final int POST_BATCH_SIZE = 20;

    @Mock
    private IndexingJobPicker indexingJobPicker;

    @Mock
    private IndexingJobExecutor indexingJobExecutor;

    private IndexingJobWorker indexingJobWorker;

    @BeforeEach
    void setUp() {
        indexingJobWorker = new IndexingJobWorker(JOB_BATCH_SIZE, POST_BATCH_SIZE, indexingJobPicker, indexingJobExecutor);
    }

    @DisplayName("poll 테스트")
    @Nested
    class PollTest {

        @Test
        void PENDING_job마다_executeAsync가_호출되고_CRON_job이_있으면_추가로_executeAsync가_호출된다() {
            UUID manualId1 = UUID.randomUUID();
            UUID manualId2 = UUID.randomUUID();
            UUID cronId = UUID.randomUUID();

            Mockito.doReturn(List.of(manualId1, manualId2)).when(indexingJobPicker).pickPendingJobs(JOB_BATCH_SIZE);
            Mockito.doReturn(cronId).when(indexingJobPicker).tryStartCronJob(POST_BATCH_SIZE);

            indexingJobWorker.poll();

            Mockito.verify(indexingJobExecutor).executeAsync(eq(manualId1));
            Mockito.verify(indexingJobExecutor).executeAsync(eq(manualId2));
            Mockito.verify(indexingJobExecutor).executeAsync(eq(cronId));
        }

        @Test
        void CRON_job이_null이면_CRON용_executeAsync는_호출되지_않는다() {
            UUID manualId = UUID.randomUUID();
            Mockito.doReturn(List.of(manualId)).when(indexingJobPicker).pickPendingJobs(JOB_BATCH_SIZE);
            Mockito.doReturn(null).when(indexingJobPicker).tryStartCronJob(POST_BATCH_SIZE);

            indexingJobWorker.poll();

            Mockito.verify(indexingJobExecutor, Mockito.times(1)).executeAsync(Mockito.any());
            Mockito.verify(indexingJobExecutor).executeAsync(eq(manualId));
        }

        @Test
        void PENDING이_비어있고_CRON도_null이면_executeAsync가_호출되지_않는다() {
            Mockito.doReturn(List.of()).when(indexingJobPicker).pickPendingJobs(JOB_BATCH_SIZE);
            Mockito.doReturn(null).when(indexingJobPicker).tryStartCronJob(POST_BATCH_SIZE);

            indexingJobWorker.poll();

            Mockito.verify(indexingJobExecutor, Mockito.never()).executeAsync(Mockito.any());
        }

        @Test
        void poll_도중_예외가_나도_예외를_밖으로_던지지_않는다() {
            Mockito.doThrow(new RuntimeException("picker failed")).when(indexingJobPicker).pickPendingJobs(JOB_BATCH_SIZE);

            indexingJobWorker.poll();

            Mockito.verify(indexingJobPicker, Mockito.never()).tryStartCronJob(Mockito.anyInt());
            Mockito.verify(indexingJobExecutor, Mockito.never()).executeAsync(Mockito.any());
        }
    }
}
