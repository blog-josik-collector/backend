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
import com.backend.integratedworker.indexingjob.repository.PostElasticsearchRepository;
import com.backend.commonelasticsearch.bulk.BulkOperationResult;
import com.backend.integratedworker.indexingjob.repository.dto.EsPostDocument;
import com.backend.integratedworker.indexingjob.service.dto.IndexingResult;
import com.backend.integratedworker.post.service.PostService;
import java.time.OffsetDateTime;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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
    private PostElasticsearchRepository postElasticsearchRepository;

    @Mock
    private CollectSourcePostService collectSourcePostService;

    @Mock
    private PostService postService;

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

    private IndexingJob cronJob() {
        return IndexingJob.builder()
                         .id(UUID.randomUUID())
                         .indexingJobType(IndexingJobType.CRON)
                         .jobStatus(JobStatus.RUNNING)
                         .totalCount(0)
                         .indexedCount(0)
                         .build();
    }

    @DisplayName("executeIndexing CRON")
    @Nested
    class CronIndexingTest {

        @Test
        void 대상_post가_없으면_ES_호출_없이_0_0을_반환한다() {
            IndexingJob job = cronJob();
            Mockito.doReturn(List.of()).when(collectSourcePostService).getIndexingCollectSourcePosts(job.id());

            IndexingResult result = indexingService.executeIndexing(job);

            Assertions.assertThat(result.totalCount()).isZero();
            Assertions.assertThat(result.indexedCount()).isZero();
            Mockito.verify(postService, Mockito.never()).createPostsIfAbsent(any());
            Mockito.verify(postElasticsearchRepository, Mockito.never()).bulkIndex(any());
            Mockito.verify(collectSourcePostService, Mockito.never())
                   .applyIndexResult(any(), any(), any(), any());
        }

        @Test
        void bulk_성공하면_TX1_ES_TX2_순서로_호출되고_indexed_카운트가_반환된다() {
            IndexingJob job = cronJob();
            CollectSourcePost post = newPost();
            BulkOperationResult bulkResult = new BulkOperationResult(Set.of(), 1);

            Mockito.doReturn(List.of(post)).when(collectSourcePostService).getIndexingCollectSourcePosts(job.id());
            Mockito.doReturn(bulkResult).when(postElasticsearchRepository).bulkIndex(any());

            IndexingResult result = indexingService.executeIndexing(job);

            Assertions.assertThat(result.totalCount()).isEqualTo(1);
            Assertions.assertThat(result.indexedCount()).isEqualTo(1);

            // 옵션B의 핵심: TX1(posts insert) → ES → TX2(mark) 순서 보장
            InOrder inOrder = Mockito.inOrder(postService, postElasticsearchRepository, collectSourcePostService);
            inOrder.verify(postService).createPostsIfAbsent(eq(List.of(post.id())));
            inOrder.verify(postElasticsearchRepository).bulkIndex(any());
            inOrder.verify(collectSourcePostService).applyIndexResult(eq(List.of(post.id())),
                                                                      eq(bulkResult),
                                                                      eq(job),
                                                                      any(OffsetDateTime.class));
        }

        @Test
        void bulk_실패한_id가_섞여있어도_applyIndexResult로_위임한다() {
            IndexingJob job = cronJob();
            CollectSourcePost p1 = newPost();
            CollectSourcePost p2 = newPost();
            BulkOperationResult bulkResult = new BulkOperationResult(Set.of(p2.id()), 1);

            Mockito.doReturn(List.of(p1, p2)).when(collectSourcePostService).getIndexingCollectSourcePosts(job.id());
            Mockito.doReturn(bulkResult).when(postElasticsearchRepository).bulkIndex(any());

            IndexingResult result = indexingService.executeIndexing(job);

            Assertions.assertThat(result.totalCount()).isEqualTo(2);
            Assertions.assertThat(result.indexedCount()).isEqualTo(1);

            // 부분 실패의 마킹 책임은 CollectSourcePostService.applyIndexResult로 위임
            Mockito.verify(collectSourcePostService).applyIndexResult(eq(List.of(p1.id(), p2.id())),
                                                                      eq(bulkResult),
                                                                      eq(job),
                                                                      any(OffsetDateTime.class));
        }

        @Test
        void ES로_보내는_documents에는_targets의_id가_모두_포함된다() {
            IndexingJob job = cronJob();
            CollectSourcePost p1 = newPost();
            CollectSourcePost p2 = newPost();

            Mockito.doReturn(List.of(p1, p2)).when(collectSourcePostService).getIndexingCollectSourcePosts(job.id());
            Mockito.doReturn(new BulkOperationResult(Set.of(), 2)).when(postElasticsearchRepository).bulkIndex(any());

            indexingService.executeIndexing(job);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<EsPostDocument>> docsCaptor = ArgumentCaptor.forClass(List.class);
            Mockito.verify(postElasticsearchRepository).bulkIndex(docsCaptor.capture());

            List<EsPostDocument> docs = docsCaptor.getValue();
            Assertions.assertThat(docs).hasSize(2);
            Assertions.assertThat(docs.stream().map(EsPostDocument::id))
                      .containsExactlyInAnyOrder(p1.id(), p2.id());
        }
    }

    @DisplayName("executeIndexing MANUAL")
    @Nested
    class ManualIndexingTest {

        private IndexingJob manualJobWithSource() {
            return IndexingJob.builder()
                             .id(UUID.randomUUID())
                             .indexingJobType(IndexingJobType.MANUAL)
                             .jobStatus(JobStatus.RUNNING)
                             .targetSource(collectSource)
                             .totalCount(0)
                             .indexedCount(0)
                             .build();
        }

        private IndexingJob manualJobWithPost(CollectSourcePost targetPost) {
            return IndexingJob.builder()
                             .id(UUID.randomUUID())
                             .indexingJobType(IndexingJobType.MANUAL)
                             .jobStatus(JobStatus.RUNNING)
                             .targetPost(targetPost)
                             .totalCount(0)
                             .indexedCount(0)
                             .build();
        }

        @Test
        void 재색인_대상이_비어있으면_ES_호출_없이_0_0을_반환한다() {
            IndexingJob job = manualJobWithSource();
            Mockito.doReturn(List.of()).when(collectSourcePostService).markIndexingBatch(job);

            IndexingResult result = indexingService.executeIndexing(job);

            Assertions.assertThat(result.totalCount()).isZero();
            Assertions.assertThat(result.indexedCount()).isZero();
            Mockito.verify(postElasticsearchRepository, Mockito.never()).bulkIndex(any());
        }

        @Test
        void targetSource로부터_대상을_받아_TX1_ES_TX2_순서로_처리한다() {
            IndexingJob job = manualJobWithSource();
            CollectSourcePost post = newPost();
            BulkOperationResult bulkResult = new BulkOperationResult(Set.of(), 1);

            Mockito.doReturn(List.of(post)).when(collectSourcePostService).markIndexingBatch(job);
            Mockito.doReturn(bulkResult).when(postElasticsearchRepository).bulkIndex(any());

            IndexingResult result = indexingService.executeIndexing(job);

            Assertions.assertThat(result.totalCount()).isEqualTo(1);
            Assertions.assertThat(result.indexedCount()).isEqualTo(1);

            InOrder inOrder = Mockito.inOrder(collectSourcePostService, postService, postElasticsearchRepository);
            inOrder.verify(collectSourcePostService).markIndexingBatch(job);
            inOrder.verify(postService).createPostsIfAbsent(eq(List.of(post.id())));
            inOrder.verify(postElasticsearchRepository).bulkIndex(any());
            inOrder.verify(collectSourcePostService).applyIndexResult(eq(List.of(post.id())),
                                                                      eq(bulkResult),
                                                                      eq(job),
                                                                      any(OffsetDateTime.class));
        }

        @Test
        void targetPost가_있으면_markIndexingBatch를_통해_단건_색인된다() {
            CollectSourcePost post = newPost();
            IndexingJob job = manualJobWithPost(post);
            BulkOperationResult bulkResult = new BulkOperationResult(Set.of(), 1);

            Mockito.doReturn(List.of(post)).when(collectSourcePostService).markIndexingBatch(job);
            Mockito.doReturn(bulkResult).when(postElasticsearchRepository).bulkIndex(any());

            IndexingResult result = indexingService.executeIndexing(job);

            Assertions.assertThat(result.totalCount()).isEqualTo(1);
            Assertions.assertThat(result.indexedCount()).isEqualTo(1);
            Mockito.verify(collectSourcePostService).markIndexingBatch(job);
        }

        @Test
        void markIndexingBatch에서_IllegalStateException이_나면_그대로_전파된다() {
            IndexingJob job = manualJobWithSource();
            Mockito.doThrow(new IllegalStateException("MANUAL job 대상이 잘못됨"))
                   .when(collectSourcePostService).markIndexingBatch(job);

            Assertions.assertThatThrownBy(() -> indexingService.executeIndexing(job))
                      .isInstanceOf(IllegalStateException.class)
                      .hasMessageContaining("MANUAL job 대상이 잘못됨");
        }
    }
}
