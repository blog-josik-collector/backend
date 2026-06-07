package com.backend.integratedworker.collectingjob.service;

import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.integratedworker.collectingjob.repository.CollectingJobQueryRepository;
import com.backend.integratedworker.collectingjob.service.crawler.kakao.KakaoPost;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import com.backend.integratedworker.collectsourcepost.service.CollectSourcePostService;
import java.time.OffsetDateTime;
import java.util.List;
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

@DisplayName("CollectingJobService 테스트")
@ExtendWith(MockitoExtension.class)
class CollectingJobServiceTest {

    @InjectMocks
    private CollectingJobService collectingJobService;

    @Mock
    private CollectingJobQueryRepository queryRepository;

    @Mock
    private CollectSourcePostService collectSourcePostService;

    private UUID jobId;
    private CollectingJob job;

    @BeforeEach
    void init() {
        jobId = UUID.randomUUID();
        job = CollectingJob.builder()
                           .id(jobId)
                           .jobStatus(JobStatus.RUNNING)
                           .totalCount(0)
                           .collectedCount(0)
                           .build();
    }

    @DisplayName("getJobForExecution 테스트")
    @Nested
    class GetJobForExecutionTest {

        @Test
        void jobId로_조회된_CollectingJob을_반환한다() {
            Mockito.doReturn(Optional.of(job)).when(queryRepository).fetchOneWithCollectSourceById(jobId);

            CollectingJob result = collectingJobService.getJobForExecution(jobId);

            Assertions.assertThat(result).isSameAs(job);
        }

        @Test
        void 존재하지_않는_jobId면_NotFoundException을_던진다() {
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneWithCollectSourceById(jobId);

            Assertions.assertThatThrownBy(() -> collectingJobService.getJobForExecution(jobId))
                      .isInstanceOf(NotFoundException.class);
        }
    }

    @DisplayName("상태 변경 테스트")
    @Nested
    class StatusUpdateTest {

        @Test
        void updateCounts는_카운트를_갱신한다() {
            Mockito.doReturn(Optional.of(job)).when(queryRepository).fetchOneById(jobId);

            collectingJobService.updateCounts(jobId, 10, 8);

            Assertions.assertThat(job.totalCount()).isEqualTo(10);
            Assertions.assertThat(job.collectedCount()).isEqualTo(8);
        }

        @Test
        void markSuccess는_Job을_SUCCESS로_마킹한다() {
            Mockito.doReturn(Optional.of(job)).when(queryRepository).fetchOneById(jobId);
            OffsetDateTime now = OffsetDateTime.now();

            collectingJobService.markSuccess(jobId, now);

            Assertions.assertThat(job.jobStatus()).isEqualTo(JobStatus.SUCCESS);
            Assertions.assertThat(job.endedAt()).isEqualTo(now);
        }

        @Test
        void completeSuccess는_카운트_갱신과_SUCCESS_마킹을_함께_적용한다() {
            Mockito.doReturn(Optional.of(job)).when(queryRepository).fetchOneById(jobId);
            OffsetDateTime now = OffsetDateTime.now();

            collectingJobService.completeSuccess(jobId, 10, 8, now);

            Assertions.assertThat(job.totalCount()).isEqualTo(10);
            Assertions.assertThat(job.collectedCount()).isEqualTo(8);
            Assertions.assertThat(job.jobStatus()).isEqualTo(JobStatus.SUCCESS);
            Assertions.assertThat(job.endedAt()).isEqualTo(now);
        }

        @Test
        void markFailed는_Job을_FAILED로_마킹한다() {
            Mockito.doReturn(Optional.of(job)).when(queryRepository).fetchOneById(jobId);
            OffsetDateTime now = OffsetDateTime.now();

            collectingJobService.markFailed(jobId, now, "error");

            Assertions.assertThat(job.jobStatus()).isEqualTo(JobStatus.FAILED);
            Assertions.assertThat(job.errorMessage()).isEqualTo("error");
        }

        @Test
        void finishCollect는_post_persist_후_SUCCESS를_마킹한다() {
            Mockito.doReturn(Optional.of(job)).when(queryRepository).fetchOneById(jobId);
            OffsetDateTime now = OffsetDateTime.now();
            UUID collectSourceId = UUID.randomUUID();
            Post post = KakaoPost.builder()
                                 .title("title")
                                 .url("https://test.com/post/1")
                                 .publishedAt(java.time.LocalDate.of(2025, 1, 1))
                                 .build();

            collectingJobService.finishCollect(jobId, collectSourceId, false, List.of(post), now);

            Mockito.verify(collectSourcePostService)
                   .persistCollectedPostsForJob(jobId, collectSourceId, false, List.of(post));
            Assertions.assertThat(job.jobStatus()).isEqualTo(JobStatus.SUCCESS);
            Assertions.assertThat(job.totalCount()).isEqualTo(1);
            Assertions.assertThat(job.collectedCount()).isEqualTo(1);
            Assertions.assertThat(job.endedAt()).isEqualTo(now);
        }
    }
}
