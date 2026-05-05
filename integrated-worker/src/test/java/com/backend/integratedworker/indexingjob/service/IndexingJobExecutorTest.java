package com.backend.integratedworker.indexingjob.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedworker.indexingjob.repository.IndexingJobQueryRepository;
import com.backend.integratedworker.indexingjob.service.dto.IndexingResult;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("IndexingJobExecutor 테스트")
@ExtendWith(MockitoExtension.class)
class IndexingJobExecutorTest {

    @Spy
    @InjectMocks
    private IndexingJobExecutor indexingJobExecutor;

    @Mock
    private IndexingJobQueryRepository queryRepository;

    @Mock
    private IndexingService indexingService;

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
        void 색인이_성공하면_total_indexed_카운트가_갱신되고_Job을_SUCCESS로_마킹한다() {
            Mockito.doReturn(Optional.of(runningJob)).when(queryRepository).fetchOneById(jobId);
            Mockito.doReturn(new IndexingResult(3, 3)).when(indexingService).executeIndexing(any(IndexingJob.class));

            indexingJobExecutor.executeAsync(jobId);

            Assertions.assertThat(runningJob.totalCount()).isEqualTo(3);
            Assertions.assertThat(runningJob.indexedCount()).isEqualTo(3);
            Assertions.assertThat(runningJob.jobStatus()).isEqualTo(JobStatus.SUCCESS);
            Assertions.assertThat(runningJob.endedAt()).isNotNull();
        }

        @Test
        void markSuccess_시점에_Job을_못_찾으면_ifPresent만_건너뛴다() {
            Mockito.doReturn(Optional.of(runningJob), Optional.empty()).when(queryRepository).fetchOneById(jobId);
            Mockito.doReturn(new IndexingResult(1, 1)).when(indexingService).executeIndexing(any(IndexingJob.class));

            indexingJobExecutor.executeAsync(jobId);

            Assertions.assertThat(runningJob.totalCount()).isEqualTo(1);
            Assertions.assertThat(runningJob.jobStatus()).isNotEqualTo(JobStatus.SUCCESS);
        }

        @Test
        void Job이_없으면_IllegalStateException으로_실패_마킹한다() {
            IndexingJob failedTarget = IndexingJob.builder()
                                                  .id(jobId)
                                                  .indexingJobType(IndexingJobType.MANUAL)
                                                  .jobStatus(JobStatus.RUNNING)
                                                  .totalCount(0)
                                                  .indexedCount(0)
                                                  .build();
            Mockito.when(queryRepository.fetchOneById(jobId))
                   .thenReturn(Optional.empty())
                   .thenReturn(Optional.of(failedTarget));

            indexingJobExecutor.executeAsync(jobId);

            Assertions.assertThat(failedTarget.jobStatus()).isEqualTo(JobStatus.FAILED);
            Assertions.assertThat(failedTarget.errorMessage()).contains("IndexingJob not found");
        }

        @Test
        void markFailed_시점에_Job을_못_찾으면_ifPresent만_건너뛴다() {
            Mockito.when(queryRepository.fetchOneById(jobId))
                   .thenReturn(Optional.of(runningJob))
                   .thenReturn(Optional.empty());
            Mockito.doThrow(new RuntimeException("es down")).when(indexingService).executeIndexing(any(IndexingJob.class));

            indexingJobExecutor.executeAsync(jobId);

            Assertions.assertThat(runningJob.jobStatus()).isEqualTo(JobStatus.RUNNING);
        }

        @Test
        void 색인_중_예외가_나면_Job을_FAILED로_마킹한다() {
            String err = "bulk failed";
            Mockito.doReturn(Optional.of(runningJob)).when(queryRepository).fetchOneById(jobId);
            Mockito.doThrow(new RuntimeException(err)).when(indexingService).executeIndexing(any(IndexingJob.class));

            indexingJobExecutor.executeAsync(jobId);

            Assertions.assertThat(runningJob.jobStatus()).isEqualTo(JobStatus.FAILED);
            Assertions.assertThat(runningJob.endedAt()).isNotNull();
            Assertions.assertThat(runningJob.errorMessage()).isEqualTo(err);
        }
    }
}
