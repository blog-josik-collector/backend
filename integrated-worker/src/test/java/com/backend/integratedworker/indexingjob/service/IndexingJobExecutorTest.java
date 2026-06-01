package com.backend.integratedworker.indexingjob.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedworker.indexingjob.service.dto.IndexingResult;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("IndexingJobExecutor 테스트")
@ExtendWith(MockitoExtension.class)
class IndexingJobExecutorTest {

    @InjectMocks
    private IndexingJobExecutor indexingJobExecutor;

    @Mock
    private IndexingService indexingService;

    @Mock
    private IndexingJobService indexingJobService;

    private UUID jobId;
    private IndexingJob runningJob;

    @BeforeEach
    void init() {
        jobId = UUID.randomUUID();
        runningJob = IndexingJob.builder()
                                .id(jobId)
                                .indexingJobType(IndexingJobType.CRON)
                                .jobStatus(JobStatus.RUNNING)
                                .totalCount(0)
                                .indexedCount(0)
                                .build();
    }

    @DisplayName("executeAsync 테스트")
    @Nested
    class ExecuteAsyncTest {

        @Test
        void 색인이_성공하면_updateCounts_그리고_markSuccess가_순서대로_호출된다() {
            Mockito.doReturn(runningJob).when(indexingJobService).getJob(jobId);
            Mockito.doReturn(new IndexingResult(3, 3)).when(indexingService).executeIndexing(runningJob);

            indexingJobExecutor.executeAsync(jobId);

            InOrder inOrder = Mockito.inOrder(indexingJobService, indexingService);
            inOrder.verify(indexingJobService).getJob(jobId);
            inOrder.verify(indexingService).executeIndexing(runningJob);
            inOrder.verify(indexingJobService).updateCounts(jobId, 3, 3);
            inOrder.verify(indexingJobService).markSuccess(eq(jobId), any(OffsetDateTime.class));

            Mockito.verify(indexingJobService, Mockito.never())
                   .markFailed(any(UUID.class), any(OffsetDateTime.class), any());
        }

        @Test
        void 색인_도중_예외가_나면_markFailed가_예외_메시지와_함께_호출된다() {
            Mockito.doReturn(runningJob).when(indexingJobService).getJob(jobId);
            Mockito.doThrow(new RuntimeException("bulk failed"))
                   .when(indexingService).executeIndexing(runningJob);

            indexingJobExecutor.executeAsync(jobId);

            Mockito.verify(indexingJobService).markFailed(eq(jobId), any(OffsetDateTime.class), eq("bulk failed"));
            Mockito.verify(indexingJobService, Mockito.never()).markSuccess(any(), any());
            Mockito.verify(indexingJobService, Mockito.never()).updateCounts(any(), anyInt(), anyInt());
        }

        @Test
        void getJob_시점에_예외가_나면_markFailed가_예외_메시지와_함께_호출된다() {
            Mockito.doThrow(new NotFoundException("존재하지 않는 indexingJob입니다. id: " + jobId))
                   .when(indexingJobService).getJob(jobId);

            indexingJobExecutor.executeAsync(jobId);

            Mockito.verify(indexingService, Mockito.never()).executeIndexing(any());
            Mockito.verify(indexingJobService).markFailed(eq(jobId), any(OffsetDateTime.class),
                                                          Mockito.contains("존재하지 않는 indexingJob입니다"));
        }

        @Test
        void updateCounts에서_예외가_나도_markFailed가_호출된다() {
            Mockito.doReturn(runningJob).when(indexingJobService).getJob(jobId);
            Mockito.doReturn(new IndexingResult(2, 2)).when(indexingService).executeIndexing(runningJob);
            Mockito.doThrow(new RuntimeException("count update failed"))
                   .when(indexingJobService).updateCounts(jobId, 2, 2);

            indexingJobExecutor.executeAsync(jobId);

            Mockito.verify(indexingJobService).markFailed(eq(jobId), any(OffsetDateTime.class), eq("count update failed"));
            Mockito.verify(indexingJobService, Mockito.never()).markSuccess(any(), any());
        }

        @Test
        void markSuccess에서_예외가_나도_markFailed가_호출된다() {
            Mockito.doReturn(runningJob).when(indexingJobService).getJob(jobId);
            Mockito.doReturn(new IndexingResult(1, 1)).when(indexingService).executeIndexing(runningJob);
            Mockito.doThrow(new RuntimeException("success mark failed"))
                   .when(indexingJobService).markSuccess(eq(jobId), any(OffsetDateTime.class));

            indexingJobExecutor.executeAsync(jobId);

            Mockito.verify(indexingJobService).markFailed(eq(jobId), any(OffsetDateTime.class), eq("success mark failed"));
        }

        @Test
        void markFailed에서_또_예외가_나면_예외가_밖으로_전파된다() {
            // 옵션B 한계점: markFailed도 실패하면 더 이상 복구 수단이 없음. uncaught로 던져진다.
            Mockito.doReturn(runningJob).when(indexingJobService).getJob(jobId);
            Mockito.doThrow(new RuntimeException("es down")).when(indexingService).executeIndexing(runningJob);
            Mockito.doThrow(new RuntimeException("DB also down"))
                   .when(indexingJobService).markFailed(eq(jobId), any(OffsetDateTime.class), Mockito.anyString());

            Assertions.assertThatThrownBy(() -> indexingJobExecutor.executeAsync(jobId))
                      .isInstanceOf(RuntimeException.class)
                      .hasMessageContaining("DB also down");
        }
    }

    @DisplayName("doIndexing 테스트")
    @Nested
    class DoIndexingTest {

        @Test
        void getJob한_뒤_executeIndexing의_결과를_그대로_반환한다() {
            Mockito.doReturn(runningJob).when(indexingJobService).getJob(jobId);
            IndexingResult expected = new IndexingResult(5, 4);
            Mockito.doReturn(expected).when(indexingService).executeIndexing(runningJob);

            IndexingResult actual = indexingJobExecutor.doIndexing(jobId);

            Assertions.assertThat(actual).isSameAs(expected);
        }
    }
}
