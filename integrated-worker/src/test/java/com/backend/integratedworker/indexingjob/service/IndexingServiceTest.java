package com.backend.integratedworker.indexingjob.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.common.enums.CollectScheduleType;
import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.IndexingStatus;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectsourcepost.service.CollectSourcePostService;
import com.backend.integratedworker.common.service.elasticsearch.ElasticsearchService;
import com.backend.integratedworker.common.service.elasticsearch.dto.BulkIndexResult;
import com.backend.integratedworker.indexingjob.service.dto.IndexingResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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

@DisplayName("IndexingService 테스트")
@ExtendWith(MockitoExtension.class)
class IndexingServiceTest {

    @InjectMocks
    private IndexingService indexingService;

    @Mock
    private ElasticsearchService elasticsearchService;

    @Mock
    private CollectSourcePostService collectSourcePostService;

    private CollectSource collectSource;
    private CollectingJob collectingJob;

    @BeforeEach
    void init() {
        PostProvider postProvider = PostProvider.builder()
                                                .id(UUID.randomUUID())
                                                .name("provider_name")
                                                .baseUrl("https://test.com")
                                                .description("d")
                                                .isUsed(true)
                                                .collectSources(new ArrayList<>())
                                                .build();

        collectSource = CollectSource.builder()
                                     .id(UUID.randomUUID())
                                     .postProvider(postProvider)
                                     .url("https://test.com/blog")
                                     .collectScheduleType(CollectScheduleType.MANUAL)
                                     .isUsed(true)
                                     .build();

        collectingJob = CollectingJob.builder()
                                     .id(UUID.randomUUID())
                                     .collectSource(collectSource)
                                     .jobStatus(JobStatus.SUCCESS)
                                     .fromPage(1)
                                     .toPage(1)
                                     .build();
    }

    private CollectSourcePost newPost() {
        return CollectSourcePost.builder()
                                .id(UUID.randomUUID())
                                .collectSource(collectSource)
                                .title("title")
                                .url("https://test.com/p/" + UUID.randomUUID())
                                .thumbnailUrl("thumb")
                                .summary("sum")
                                .lastCollectingJob(collectingJob)
                                .indexingStatus(IndexingStatus.INDEXING)
                                .build();
    }

    @DisplayName("executeIndexing CRON")
    @Nested
    class CronIndexingTest {

        @Test
        void 대상_post가_없으면_0_0_결과를_반환한다() {
            UUID jobId = UUID.randomUUID();
            IndexingJob job = IndexingJob.builder()
                                         .id(jobId)
                                         .indexingJobType(IndexingJobType.CRON)
                                         .jobStatus(JobStatus.RUNNING)
                                         .totalCount(0)
                                         .indexedCount(0)
                                         .build();

            Mockito.doReturn(List.of()).when(collectSourcePostService).getIndexingCollectSourcePosts(jobId);

            IndexingResult result = indexingService.executeIndexing(job);

            Assertions.assertThat(result.totalCount()).isZero();
            Assertions.assertThat(result.indexedCount()).isZero();
            Mockito.verify(elasticsearchService, Mockito.never()).bulkIndex(any());
        }

        @Test
        void bulk_성공하면_indexed_카운트가_반영되고_post가_INDEXED로_마킹된다() {
            UUID jobId = UUID.randomUUID();
            IndexingJob job = IndexingJob.builder()
                                         .id(jobId)
                                         .indexingJobType(IndexingJobType.CRON)
                                         .jobStatus(JobStatus.RUNNING)
                                         .totalCount(0)
                                         .indexedCount(0)
                                         .build();
            CollectSourcePost post = newPost();

            Mockito.doReturn(List.of(post)).when(collectSourcePostService).getIndexingCollectSourcePosts(jobId);
            Mockito.doReturn(new BulkIndexResult(Set.of(), 1)).when(elasticsearchService).bulkIndex(any());

            IndexingResult result = indexingService.executeIndexing(job);

            Assertions.assertThat(result.totalCount()).isEqualTo(1);
            Assertions.assertThat(result.indexedCount()).isEqualTo(1);
            Assertions.assertThat(post.indexingStatus()).isEqualTo(IndexingStatus.INDEXED);
        }

        @Test
        void bulk_실패한_id는_post를_FAILED로_마킹한다() {
            UUID jobId = UUID.randomUUID();
            IndexingJob job = IndexingJob.builder()
                                         .id(jobId)
                                         .indexingJobType(IndexingJobType.CRON)
                                         .jobStatus(JobStatus.RUNNING)
                                         .totalCount(0)
                                         .indexedCount(0)
                                         .build();
            CollectSourcePost post = newPost();

            Mockito.doReturn(List.of(post)).when(collectSourcePostService).getIndexingCollectSourcePosts(jobId);
            Mockito.doReturn(new BulkIndexResult(Set.of(post.id()), 0)).when(elasticsearchService).bulkIndex(any());

            IndexingResult result = indexingService.executeIndexing(job);

            Assertions.assertThat(result.totalCount()).isEqualTo(1);
            Assertions.assertThat(result.indexedCount()).isZero();
            Assertions.assertThat(post.indexingStatus()).isEqualTo(IndexingStatus.FAILED);
        }
    }

    @DisplayName("executeIndexing MANUAL")
    @Nested
    class ManualIndexingTest {

        @Test
        void 재색인_대상이_비어있으면_ES_호출_없이_0_0을_반환한다() {
            UUID jobId = UUID.randomUUID();
            IndexingJob job = IndexingJob.builder()
                                         .id(jobId)
                                         .indexingJobType(IndexingJobType.MANUAL)
                                         .jobStatus(JobStatus.RUNNING)
                                         .targetSource(collectSource)
                                         .totalCount(0)
                                         .indexedCount(0)
                                         .build();

            Mockito.doReturn(List.of()).when(collectSourcePostService).getReindexTargetCollectSourcePosts(collectSource.id());

            IndexingResult result = indexingService.executeIndexing(job);

            Assertions.assertThat(result.totalCount()).isZero();
            Assertions.assertThat(result.indexedCount()).isZero();
            Mockito.verify(elasticsearchService, Mockito.never()).bulkIndex(any());
        }

        @Test
        void targetSource만_있으면_재색인_대상을_가져와_INDEXING_후_bulk_처리한다() {
            UUID jobId = UUID.randomUUID();
            IndexingJob job = IndexingJob.builder()
                                         .id(jobId)
                                         .indexingJobType(IndexingJobType.MANUAL)
                                         .jobStatus(JobStatus.RUNNING)
                                         .targetSource(collectSource)
                                         .totalCount(0)
                                         .indexedCount(0)
                                         .build();
            CollectSourcePost post = newPost();

            Mockito.doReturn(List.of(post)).when(collectSourcePostService).getReindexTargetCollectSourcePosts(collectSource.id());
            Mockito.doReturn(new BulkIndexResult(Set.of(), 1)).when(elasticsearchService).bulkIndex(any());

            IndexingResult result = indexingService.executeIndexing(job);

            Assertions.assertThat(result.indexedCount()).isEqualTo(1);
            Assertions.assertThat(post.indexingStatus()).isEqualTo(IndexingStatus.INDEXED);
            Mockito.verify(collectSourcePostService).getReindexTargetCollectSourcePosts(eq(collectSource.id()));
        }

        @Test
        void targetPost만_있으면_해당_post_한_건을_색인한다() {
            UUID jobId = UUID.randomUUID();
            CollectSourcePost post = newPost();
            IndexingJob job = IndexingJob.builder()
                                         .id(jobId)
                                         .indexingJobType(IndexingJobType.MANUAL)
                                         .jobStatus(JobStatus.RUNNING)
                                         .targetPost(post)
                                         .totalCount(0)
                                         .indexedCount(0)
                                         .build();

            Mockito.doReturn(new BulkIndexResult(Set.of(), 1)).when(elasticsearchService).bulkIndex(any());

            IndexingResult result = indexingService.executeIndexing(job);

            Assertions.assertThat(result.totalCount()).isEqualTo(1);
            Assertions.assertThat(post.indexingStatus()).isEqualTo(IndexingStatus.INDEXED);
            Mockito.verify(collectSourcePostService, Mockito.never()).getReindexTargetCollectSourcePosts(any());
        }

        @Test
        void targetSource와_targetPost가_모두_null이면_예외() {
            IndexingJob job = IndexingJob.builder()
                                         .id(UUID.randomUUID())
                                         .indexingJobType(IndexingJobType.MANUAL)
                                         .jobStatus(JobStatus.PENDING)
                                         .totalCount(0)
                                         .indexedCount(0)
                                         .build();

            Assertions.assertThatThrownBy(() -> indexingService.executeIndexing(job))
                      .isInstanceOf(IllegalStateException.class)
                      .hasMessageContaining("MANUAL job 대상이 잘못됨");
        }

        @Test
        void targetSource와_targetPost가_모두_있으면_예외() {
            CollectSourcePost post = newPost();
            IndexingJob job = IndexingJob.builder()
                                         .id(UUID.randomUUID())
                                         .indexingJobType(IndexingJobType.MANUAL)
                                         .jobStatus(JobStatus.PENDING)
                                         .targetSource(collectSource)
                                         .targetPost(post)
                                         .totalCount(0)
                                         .indexedCount(0)
                                         .build();

            Assertions.assertThatThrownBy(() -> indexingService.executeIndexing(job))
                      .isInstanceOf(IllegalStateException.class)
                      .hasMessageContaining("MANUAL job 대상이 잘못됨");
        }
    }
}
