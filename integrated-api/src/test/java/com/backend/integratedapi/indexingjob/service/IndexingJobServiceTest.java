package com.backend.integratedapi.indexingjob.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedapi.collectsource.service.CollectSourceService;
import com.backend.integratedapi.collectsourcepost.service.CollectSourcePostService;
import com.backend.integratedapi.indexingjob.repository.IndexingJobQueryRepository;
import com.backend.integratedapi.indexingjob.repository.IndexingJobRepository;
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

@DisplayName("IndexingJobService 테스트")
@ExtendWith(MockitoExtension.class)
class IndexingJobServiceTest {

    @Spy
    @InjectMocks
    private IndexingJobService indexingJobService;

    @Mock
    private IndexingJobRepository indexingJobRepository;

    @Mock
    private IndexingJobQueryRepository queryRepository;

    @Mock
    private CollectSourceService collectSourceService;

    @Mock
    private CollectSourcePostService collectSourcePostService;

    private CollectSource collectSource;
    private CollectSourcePost collectSourcePost;

    @BeforeEach
    void init() {
        collectSource = CollectSource.builder()
                                     .id(UUID.randomUUID())
                                     .isUsed(true)
                                     .build();

        collectSourcePost = CollectSourcePost.builder()
                                             .id(UUID.randomUUID())
                                             .collectSource(collectSource)
                                             .title("t")
                                             .url("https://example.com/p")
                                             .build();
    }

    @DisplayName("CollectSource 단위 재색인")
    @Nested
    class TriggerReindexByCollectSourceTest {

        @Test
        void MANUAL_PENDING_Job을_저장하고_반환한다() {
            UUID userId = UUID.randomUUID();
            IndexingJob saved = IndexingJob.builder()
                                           .id(UUID.randomUUID())
                                           .indexingJobType(IndexingJobType.MANUAL)
                                           .jobStatus(JobStatus.PENDING)
                                           .targetSource(collectSource)
                                           .triggeredBy(userId)
                                           .build();

            Mockito.doReturn(collectSource).when(collectSourceService).getCollectSource(collectSource.id());
            Mockito.doReturn(saved).when(indexingJobRepository).save(any(IndexingJob.class));

            IndexingJob result = indexingJobService.triggerReindexByCollectSource(collectSource.id(), userId);

            Assertions.assertThat(result.id()).isEqualTo(saved.id());
            Assertions.assertThat(result.indexingJobType()).isEqualTo(IndexingJobType.MANUAL);
            Assertions.assertThat(result.jobStatus()).isEqualTo(JobStatus.PENDING);
            Assertions.assertThat(result.targetSource()).isEqualTo(collectSource);
            Assertions.assertThat(result.triggeredBy()).isEqualTo(userId);
        }
    }

    @DisplayName("CollectSourcePost 단위 재색인")
    @Nested
    class TriggerReindexByCollectSourcePostTest {

        @Test
        void 진행_중인_MANUAL_Job이_없으면_PENDING_Job을_저장한다() {
            UUID userId = UUID.randomUUID();
            IndexingJob saved = IndexingJob.builder()
                                           .id(UUID.randomUUID())
                                           .indexingJobType(IndexingJobType.MANUAL)
                                           .jobStatus(JobStatus.PENDING)
                                           .targetPost(collectSourcePost)
                                           .triggeredBy(userId)
                                           .build();

            Mockito.doReturn(collectSourcePost).when(collectSourcePostService).getCollectSourcePost(collectSourcePost.id());
            Mockito.doReturn(false).when(queryRepository).existsActiveManualJobForPost(collectSourcePost.id());
            Mockito.doReturn(saved).when(indexingJobRepository).save(any(IndexingJob.class));

            IndexingJob result = indexingJobService.triggerReindexByCollectSourcePost(collectSourcePost.id(), userId);

            Assertions.assertThat(result).isSameAs(saved);
            Assertions.assertThat(result.jobStatus()).isEqualTo(JobStatus.PENDING);
        }

        @Test
        void 이미_진행_중인_MANUAL_Job이_있으면_예외() {
            UUID userId = UUID.randomUUID();

            Mockito.doReturn(collectSourcePost).when(collectSourcePostService).getCollectSourcePost(collectSourcePost.id());
            Mockito.doReturn(true).when(queryRepository).existsActiveManualJobForPost(collectSourcePost.id());

            Assertions.assertThatThrownBy(
                              () -> indexingJobService.triggerReindexByCollectSourcePost(collectSourcePost.id(), userId))
                      .isInstanceOf(IllegalStateException.class)
                      .hasMessageContaining("이미 진행 중인 재색인 작업이 있음");
        }
    }
}
