package com.backend.integratedworker.indexingjob.service;

import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedworker.indexingjob.repository.IndexingJobQueryRepository;
import java.time.OffsetDateTime;
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
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("IndexingJobService 테스트")
@ExtendWith(MockitoExtension.class)
class IndexingJobServiceTest {

    @InjectMocks
    private IndexingJobService indexingJobService;

    @Mock
    private IndexingJobQueryRepository queryRepository;

    private UUID jobId;
    private IndexingJob job;

    @BeforeEach
    void init() {
        jobId = UUID.randomUUID();
        job = IndexingJob.builder()
                         .id(jobId)
                         .indexingJobType(IndexingJobType.CRON)
                         .jobStatus(JobStatus.RUNNING)
                         .totalCount(0)
                         .indexedCount(0)
                         .build();
    }

    @DisplayName("getJob 테스트")
    @Nested
    class GetJobTest {

        @Test
        void jobId로_조회된_IndexingJob을_반환한다() {
            Mockito.doReturn(Optional.of(job)).when(queryRepository).fetchOneById(jobId);

            IndexingJob result = indexingJobService.getJob(jobId);

            Assertions.assertThat(result).isSameAs(job);
        }

        @Test
        void jobId가_없으면_NotFoundException을_던진다() {
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneById(jobId);

            Assertions.assertThatThrownBy(() -> indexingJobService.getJob(jobId))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 indexingJob입니다");
        }
    }

    @DisplayName("updateCounts 테스트")
    @Nested
    class UpdateCountsTest {

        @Test
        void totalCount와_indexedCount가_갱신된다() {
            Mockito.doReturn(Optional.of(job)).when(queryRepository).fetchOneById(jobId);

            indexingJobService.updateCounts(jobId, 10, 7);

            Assertions.assertThat(job.totalCount()).isEqualTo(10);
            Assertions.assertThat(job.indexedCount()).isEqualTo(7);
        }

        @Test
        void jobId가_없으면_예외가_전파된다() {
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneById(jobId);

            Assertions.assertThatThrownBy(() -> indexingJobService.updateCounts(jobId, 1, 1))
                      .isInstanceOf(NotFoundException.class);
        }
    }

    @DisplayName("markSuccess 테스트")
    @Nested
    class MarkSuccessTest {

        @Test
        void jobStatus가_SUCCESS로_endedAt이_지금으로_설정된다() {
            Mockito.doReturn(Optional.of(job)).when(queryRepository).fetchOneById(jobId);
            OffsetDateTime now = OffsetDateTime.now();

            indexingJobService.markSuccess(jobId, now);

            Assertions.assertThat(job.jobStatus()).isEqualTo(JobStatus.SUCCESS);
            Assertions.assertThat(job.endedAt()).isEqualTo(now);
        }

        @Test
        void jobId가_없으면_예외가_전파된다() {
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneById(jobId);

            Assertions.assertThatThrownBy(() -> indexingJobService.markSuccess(jobId, OffsetDateTime.now()))
                      .isInstanceOf(NotFoundException.class);
        }
    }

    @DisplayName("completeSuccess 테스트")
    @Nested
    class CompleteSuccessTest {

        @Test
        void 카운트_갱신과_SUCCESS_마킹을_함께_적용한다() {
            Mockito.doReturn(Optional.of(job)).when(queryRepository).fetchOneById(jobId);
            OffsetDateTime now = OffsetDateTime.now();

            indexingJobService.completeSuccess(jobId, 10, 7, now);

            Assertions.assertThat(job.totalCount()).isEqualTo(10);
            Assertions.assertThat(job.indexedCount()).isEqualTo(7);
            Assertions.assertThat(job.jobStatus()).isEqualTo(JobStatus.SUCCESS);
            Assertions.assertThat(job.endedAt()).isEqualTo(now);
        }
    }

    @DisplayName("markFailed 테스트")
    @Nested
    class MarkFailedTest {

        @Test
        void jobStatus가_FAILED로_endedAt과_errorMessage가_설정된다() {
            Mockito.doReturn(Optional.of(job)).when(queryRepository).fetchOneById(jobId);
            OffsetDateTime now = OffsetDateTime.now();

            indexingJobService.markFailed(jobId, now, "bulk failed");

            Assertions.assertThat(job.jobStatus()).isEqualTo(JobStatus.FAILED);
            Assertions.assertThat(job.endedAt()).isEqualTo(now);
            Assertions.assertThat(job.errorMessage()).isEqualTo("bulk failed");
        }

        @Test
        void jobId가_없으면_예외가_전파된다() {
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneById(jobId);

            Assertions.assertThatThrownBy(() -> indexingJobService.markFailed(jobId, OffsetDateTime.now(), "err"))
                      .isInstanceOf(NotFoundException.class);
        }
    }
}
