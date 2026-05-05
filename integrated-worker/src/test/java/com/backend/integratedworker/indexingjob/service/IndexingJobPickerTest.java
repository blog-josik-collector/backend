package com.backend.integratedworker.indexingjob.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.common.enums.CollectScheduleType;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectsourcepost.service.CollectSourcePostService;
import com.backend.integratedworker.indexingjob.repository.IndexingJobRepository;
import java.util.ArrayList;
import java.util.List;
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

@DisplayName("IndexingJobPicker 테스트")
@ExtendWith(MockitoExtension.class)
class IndexingJobPickerTest {

    @Spy
    @InjectMocks
    private IndexingJobPicker indexingJobPicker;

    @Mock
    private IndexingJobRepository indexingJobRepository;

    @Mock
    private CollectSourcePostService collectSourcePostService;

    private CollectSource collectSource;

    @BeforeEach
    void init() {
        PostProvider postProvider = PostProvider.builder()
                                                .id(UUID.randomUUID())
                                                .name("test_provider")
                                                .baseUrl("https://test.com")
                                                .description("test_description")
                                                .isUsed(true)
                                                .collectSources(new ArrayList<>())
                                                .build();

        collectSource = CollectSource.builder()
                                     .id(UUID.randomUUID())
                                     .postProvider(postProvider)
                                     .url("https://test.com/blog/1")
                                     .collectScheduleType(CollectScheduleType.MANUAL)
                                     .isUsed(true)
                                     .build();
    }

    private IndexingJob pendingJob() {
        return IndexingJob.builder()
                          .id(UUID.randomUUID())
                          .indexingJobType(IndexingJobType.MANUAL)
                          .jobStatus(JobStatus.PENDING)
                          .totalCount(0)
                          .indexedCount(0)
                          .build();
    }

    @DisplayName("pickPendingJobs 테스트")
    @Nested
    class PickPendingJobsTest {

        @Test
        void PENDING_Job을_픽업하고_RUNNING으로_마킹한_뒤_id_목록을_반환한다() {
            IndexingJob job1 = pendingJob();
            IndexingJob job2 = pendingJob();

            Mockito.doReturn(List.of(job1, job2)).when(indexingJobRepository).findAllPendingIndexingJobs(anyInt());

            Assertions.assertThat(job1.jobStatus()).isEqualTo(JobStatus.PENDING);
            Assertions.assertThat(job2.jobStatus()).isEqualTo(JobStatus.PENDING);

            List<UUID> result = indexingJobPicker.pickPendingJobs(10);

            Assertions.assertThat(result).containsExactlyInAnyOrder(job1.id(), job2.id());
            Assertions.assertThat(job1.jobStatus()).isEqualTo(JobStatus.RUNNING);
            Assertions.assertThat(job2.jobStatus()).isEqualTo(JobStatus.RUNNING);
            Assertions.assertThat(job1.startedAt()).isNotNull();
            Assertions.assertThat(job2.startedAt()).isNotNull();
        }

        @Test
        void PENDING이_없으면_빈_리스트를_반환한다() {
            Mockito.doReturn(List.of()).when(indexingJobRepository).findAllPendingIndexingJobs(anyInt());

            List<UUID> result = indexingJobPicker.pickPendingJobs(10);

            Assertions.assertThat(result).isEmpty();
        }
    }

    @DisplayName("tryStartCronJob 테스트")
    @Nested
    class TryStartCronJobTest {

        @Test
        void PENDING_post가_없으면_null을_반환한다() {
            Mockito.doReturn(List.of()).when(collectSourcePostService).pickPendingForIndexing(anyInt());

            UUID result = indexingJobPicker.tryStartCronJob(10);

            Assertions.assertThat(result).isNull();
            Mockito.verify(indexingJobRepository, Mockito.never()).save(any());
        }

        @Test
        void PENDING_post가_있으면_CRON_Job을_저장하고_post를_INDEXING으로_묶어_jobId를_반환한다() {
            UUID savedJobId = UUID.randomUUID();
            CollectingJob collectingJob = CollectingJob.builder()
                                                       .id(UUID.randomUUID())
                                                       .collectSource(collectSource)
                                                       .jobStatus(JobStatus.SUCCESS)
                                                       .fromPage(1)
                                                       .toPage(1)
                                                       .build();
            CollectSourcePost post = CollectSourcePost.builder()
                                                      .id(UUID.randomUUID())
                                                      .collectSource(collectSource)
                                                      .title("t")
                                                      .url("https://example.com/p")
                                                      .lastCollectingJob(collectingJob)
                                                      .build();

            Mockito.doReturn(List.of(post)).when(collectSourcePostService).pickPendingForIndexing(anyInt());

            Mockito.when(indexingJobRepository.save(Mockito.any(IndexingJob.class)))
                   .thenAnswer(invocation -> {
                       IndexingJob arg = invocation.getArgument(0);
                       return IndexingJob.builder()
                                         .id(savedJobId)
                                         .indexingJobType(arg.indexingJobType())
                                         .jobStatus(arg.jobStatus())
                                         .totalCount(arg.totalCount())
                                         .indexedCount(arg.indexedCount())
                                         .startedAt(arg.startedAt())
                                         .build();
                   });

            UUID result = indexingJobPicker.tryStartCronJob(10);

            Assertions.assertThat(result).isEqualTo(savedJobId);
            Assertions.assertThat(post.lastIndexingJob()).isNotNull();
            Assertions.assertThat(post.lastIndexingJob().id()).isEqualTo(savedJobId);
            Mockito.verify(indexingJobRepository).save(Mockito.any(IndexingJob.class));
        }
    }
}
