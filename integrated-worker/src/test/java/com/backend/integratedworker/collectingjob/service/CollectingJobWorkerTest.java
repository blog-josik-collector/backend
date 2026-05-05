package com.backend.integratedworker.collectingjob.service;

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

@DisplayName("CollectingJobWorker 테스트")
@ExtendWith(MockitoExtension.class)
class CollectingJobWorkerTest {

    private static final int JOB_BATCH_SIZE = 10;

    @Mock
    private CollectingJobPicker picker;

    @Mock
    private CollectingJobExecutor executor;

    private CollectingJobWorker collectingJobWorker;

    @BeforeEach
    void setUp() {
        collectingJobWorker = new CollectingJobWorker(JOB_BATCH_SIZE, picker, executor);
    }

    @DisplayName("CollectingJob poll 테스트")
    @Nested
    class PollTest {

        @Test
        void picker가_반환한_jobId마다_executor의_executeAsync가_호출된다() {
            // given
            UUID jobId1 = UUID.randomUUID();
            UUID jobId2 = UUID.randomUUID();

            Mockito.doReturn(List.of(jobId1, jobId2)).when(picker).pickAndMarkRunning(JOB_BATCH_SIZE);

            // when
            collectingJobWorker.poll();

            // then
            Mockito.verify(executor).executeAsync(eq(jobId1));
            Mockito.verify(executor).executeAsync(eq(jobId2));
        }

        @Test
        void picker가_빈_리스트를_반환하면_executor의_executeAsync가_호출되지_않는다() {
            // given
            Mockito.doReturn(List.of()).when(picker).pickAndMarkRunning(JOB_BATCH_SIZE);

            // when
            collectingJobWorker.poll();

            // then
            Mockito.verify(executor, Mockito.never()).executeAsync(Mockito.any());
        }
    }
}
