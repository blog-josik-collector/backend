package com.backend.integratedworker.collectsourcepost.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
import com.backend.integratedworker.collectingjob.service.crawler.kakao.KakaoPost;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import com.backend.integratedworker.collectsourcepost.repository.CollectSourcePostQueryRepository;
import com.backend.integratedworker.collectsourcepost.repository.CollectSourcePostRepository;
import com.backend.commonelasticsearch.bulk.BulkOperationResult;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("(Worker) CollectSourcePostService 테스트")
@ExtendWith(MockitoExtension.class)
class CollectSourcePostServiceTest {

    @Spy
    @InjectMocks
    private CollectSourcePostService collectSourcePostService;

    @Mock
    private CollectSourcePostRepository collectSourcePostRepository;

    @Mock
    private CollectSourcePostQueryRepository queryRepository;

    private PostProvider postProvider;
    private CollectSource collectSource;
    private CollectingJob collectingJob;

    @BeforeEach
    void init() {
        postProvider = PostProvider.builder()
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

        collectingJob = CollectingJob.builder()
                                     .id(UUID.randomUUID())
                                     .collectSource(collectSource)
                                     .jobStatus(JobStatus.RUNNING)
                                     .build();
    }

    private KakaoPost newPost(String url) {
        return KakaoPost.builder()
                        .title("test_title")
                        .url(url)
                        .publishedAt(LocalDate.of(2025, 1, 1))
                        .thumbnailUrl(Optional.of("https://test.com/thumb.png"))
                        .summary(Optional.of("test_summary"))
                        .build();
    }

    @DisplayName("CollectSourcePost 생성 테스트")
    @Nested
    class CreateCollectSourcePostTest {

        @Test
        void Post로부터_CollectSourcePost를_생성할_수_있다() {
            // given
            Post post = newPost("https://test.com/blog/1/post/1");

            CollectSourcePost saved = CollectSourcePost.builder()
                                                       .id(UUID.randomUUID())
                                                       .collectSource(collectSource)
                                                       .title(post.getTitle())
                                                       .url(post.getUrl())
                                                       .lastCollectingJob(collectingJob)
                                                       .build();

            Mockito.doReturn(saved).when(collectSourcePostRepository).save(any());

            // when
            CollectSourcePost result = collectSourcePostService.create(post, collectSource, collectingJob);

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(saved.id());
        }

        @Test
        void 썸네일과_요약이_없는_Post로도_CollectSourcePost를_생성할_수_있다() {
            // given
            Post post = KakaoPost.builder()
                                 .title("test_title")
                                 .url("https://test.com/blog/1/post/2")
                                 .publishedAt(LocalDate.of(2025, 1, 1))
                                 .thumbnailUrl(Optional.empty())
                                 .summary(Optional.empty())
                                 .build();

            CollectSourcePost saved = CollectSourcePost.builder()
                                                       .id(UUID.randomUUID())
                                                       .collectSource(collectSource)
                                                       .title(post.getTitle())
                                                       .url(post.getUrl())
                                                       .lastCollectingJob(collectingJob)
                                                       .build();

            Mockito.doReturn(saved).when(collectSourcePostRepository).save(any());

            // when
            CollectSourcePost result = collectSourcePostService.create(post, collectSource, collectingJob);

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(saved.id());
        }
    }

    @DisplayName("CollectSourcePost 콘텐츠 해시 생성 테스트")
    @Nested
    class CreateContentHashTest {

        @Test
        void 동일한_Post_내용은_동일한_해시값을_생성한다() {
            // given
            Post post1 = newPost("https://test.com/blog/1/post/1");
            Post post2 = newPost("https://test.com/blog/1/post/2");

            // when
            String hash1 = collectSourcePostService.createContentHash(post1);
            String hash2 = collectSourcePostService.createContentHash(post2);

            // then (URL은 해시에 포함되지 않으므로 동일해야 함)
            Assertions.assertThat(hash1).isEqualTo(hash2);
            Assertions.assertThat(hash1).hasSize(64); // SHA-256 hex
        }

        @Test
        void 서로_다른_Post_내용은_서로_다른_해시값을_생성한다() {
            // given
            Post post1 = newPost("https://test.com/blog/1/post/1");
            Post post2 = KakaoPost.builder()
                                  .title("different_title")
                                  .url("https://test.com/blog/1/post/1")
                                  .publishedAt(LocalDate.of(2025, 1, 1))
                                  .thumbnailUrl(Optional.of("https://test.com/thumb.png"))
                                  .summary(Optional.of("test_summary"))
                                  .build();

            // when
            String hash1 = collectSourcePostService.createContentHash(post1);
            String hash2 = collectSourcePostService.createContentHash(post2);

            // then
            Assertions.assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        void 썸네일과_요약이_비어있어도_해시값을_생성할_수_있다() {
            // given
            Post post = KakaoPost.builder()
                                 .title("test_title")
                                 .url("https://test.com/blog/1/post/3")
                                 .publishedAt(LocalDate.of(2025, 1, 1))
                                 .thumbnailUrl(Optional.empty())
                                 .summary(Optional.empty())
                                 .build();

            // when
            String hash = collectSourcePostService.createContentHash(post);

            // then
            Assertions.assertThat(hash).isNotBlank();
            Assertions.assertThat(hash).hasSize(64);
        }
    }

    @DisplayName("CollectSourcePost 조회 테스트")
    @Nested
    class ReadCollectSourcePostTest {

        @Test
        void id를_입력하면_CollectSourcePost를_조회할_수_있다() {
            // given
            CollectSourcePost collectSourcePost = CollectSourcePost.builder()
                                                                   .id(UUID.randomUUID())
                                                                   .collectSource(collectSource)
                                                                   .title("test_title")
                                                                   .url("https://test.com/blog/1/post/1")
                                                                   .lastCollectingJob(collectingJob)
                                                                   .build();

            Mockito.doReturn(Optional.of(collectSourcePost)).when(queryRepository).fetchOneById(any());

            // when
            CollectSourcePost result = collectSourcePostService.getCollectSourcePost(collectSourcePost.id());

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(collectSourcePost.id());
        }

        @Test
        void id가_null이면_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> collectSourcePostService.getCollectSourcePost((UUID) null))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }

        @Test
        void 존재하지_않는_id로_조회하면_조회에_실패한다() {
            // given
            UUID id = UUID.randomUUID();
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> collectSourcePostService.getCollectSourcePost(id))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("존재하지 않는 id입니다.");
        }

        @Test
        void url을_입력하면_CollectSourcePost를_조회할_수_있다() {
            // given
            String url = "https://test.com/blog/1/post/1";
            CollectSourcePost collectSourcePost = CollectSourcePost.builder()
                                                                   .id(UUID.randomUUID())
                                                                   .collectSource(collectSource)
                                                                   .title("test_title")
                                                                   .url(url)
                                                                   .lastCollectingJob(collectingJob)
                                                                   .build();

            Mockito.doReturn(Optional.of(collectSourcePost)).when(queryRepository).fetchOneByUrl(any());

            // when
            CollectSourcePost result = collectSourcePostService.getCollectSourcePost(url);

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.url()).isEqualTo(url);
        }

        @Test
        void 존재하지_않는_url로_조회하면_null을_반환한다() {
            // given
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneByUrl(any());

            // when
            CollectSourcePost result = collectSourcePostService.getCollectSourcePost("https://test.com/blog/1/post/none");

            // then
            Assertions.assertThat(result).isNull();
        }

        @Test
        void url이_빈값이면_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> collectSourcePostService.getCollectSourcePost(""))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("url은 필수 입력값입니다.");
        }
    }

    @DisplayName("CollectSourcePost 수정 테스트")
    @Nested
    class UpdateCollectSourcePostTest {

        @Test
        void Post를_입력하면_CollectSourcePost의_필드들이_갱신된다() {
            // given
            CollectSourcePost existing = CollectSourcePost.builder()
                                                          .id(UUID.randomUUID())
                                                          .collectSource(collectSource)
                                                          .title("old_title")
                                                          .url("https://test.com/blog/1/post/1")
                                                          .publishedAt(LocalDate.of(2024, 1, 1))
                                                          .thumbnailUrl("https://test.com/old_thumb.png")
                                                          .summary("old_summary")
                                                          .contentHash("old_hash")
                                                          .lastCollectingJob(collectingJob)
                                                          .build();

            Post post = KakaoPost.builder()
                                 .title("new_title")
                                 .url(existing.url())
                                 .publishedAt(LocalDate.of(2025, 6, 1))
                                 .thumbnailUrl(Optional.of("https://test.com/new_thumb.png"))
                                 .summary(Optional.of("new_summary"))
                                 .build();

            Mockito.doReturn(Optional.of(existing)).when(queryRepository).fetchOneById(any());

            // when
            collectSourcePostService.update(existing.id(), post, collectingJob);

            // then
            Assertions.assertThat(existing.title()).isEqualTo("new_title");
            Assertions.assertThat(existing.publishedAt()).isEqualTo(LocalDate.of(2025, 6, 1));
            Assertions.assertThat(existing.thumbnailUrl()).isEqualTo("https://test.com/new_thumb.png");
            Assertions.assertThat(existing.summary()).isEqualTo("new_summary");
            Assertions.assertThat(existing.contentHash()).isNotEqualTo("old_hash");
            Assertions.assertThat(existing.lastCollectingJob().id()).isEqualTo(collectingJob.id());
            Assertions.assertThat(existing.lastCollectedAt()).isNotNull();
        }

        @Test
        void 썸네일과_요약이_없는_Post로_수정하면_null로_갱신된다() {
            // given
            CollectSourcePost existing = CollectSourcePost.builder()
                                                          .id(UUID.randomUUID())
                                                          .collectSource(collectSource)
                                                          .title("old_title")
                                                          .url("https://test.com/blog/1/post/1")
                                                          .thumbnailUrl("https://test.com/old_thumb.png")
                                                          .summary("old_summary")
                                                          .contentHash("old_hash")
                                                          .lastCollectingJob(collectingJob)
                                                          .build();

            Post post = KakaoPost.builder()
                                 .title("new_title")
                                 .url(existing.url())
                                 .publishedAt(LocalDate.of(2025, 6, 1))
                                 .thumbnailUrl(Optional.empty())
                                 .summary(Optional.empty())
                                 .build();

            Mockito.doReturn(Optional.of(existing)).when(queryRepository).fetchOneById(any());

            // when
            collectSourcePostService.update(existing.id(), post, collectingJob);

            // then
            Assertions.assertThat(existing.thumbnailUrl()).isNull();
            Assertions.assertThat(existing.summary()).isNull();
        }
    }

    private CollectSourcePost newCollectSourcePost(IndexingStatus status) {
        return CollectSourcePost.builder()
                                .id(UUID.randomUUID())
                                .collectSource(collectSource)
                                .title("title")
                                .url("https://test.com/blog/1/post/" + UUID.randomUUID())
                                .lastCollectingJob(collectingJob)
                                .indexingStatus(status)
                                .build();
    }

    private IndexingJob runningIndexingJob(IndexingJobType type) {
        return IndexingJob.builder()
                          .id(UUID.randomUUID())
                          .indexingJobType(type)
                          .jobStatus(JobStatus.RUNNING)
                          .totalCount(0)
                          .indexedCount(0)
                          .build();
    }

    @DisplayName("pickPendingForIndexing 테스트")
    @Nested
    class PickPendingForIndexingTest {

        @Test
        void repository에서_PENDING_post를_조회해_반환한다() {
            CollectSourcePost p1 = newCollectSourcePost(IndexingStatus.PENDING);
            CollectSourcePost p2 = newCollectSourcePost(IndexingStatus.PENDING);
            Mockito.doReturn(List.of(p1, p2))
                   .when(collectSourcePostRepository).findAllPendingCollectSourcePosts(50);

            List<CollectSourcePost> result = collectSourcePostService.pickPendingForIndexing(50);

            Assertions.assertThat(result).containsExactly(p1, p2);
        }
    }

    @DisplayName("getIndexingCollectSourcePosts 테스트")
    @Nested
    class GetIndexingCollectSourcePostsTest {

        @Test
        void queryRepository로_위임해서_결과를_그대로_반환한다() {
            UUID jobId = UUID.randomUUID();
            CollectSourcePost p = newCollectSourcePost(IndexingStatus.INDEXING);
            Mockito.doReturn(List.of(p)).when(queryRepository).fetchIndexingCollectSourcePosts(jobId);

            List<CollectSourcePost> result = collectSourcePostService.getIndexingCollectSourcePosts(jobId);

            Assertions.assertThat(result).containsExactly(p);
        }
    }

    @DisplayName("getReindexTargetCollectSourcePosts 테스트")
    @Nested
    class GetReindexTargetCollectSourcePostsTest {

        @Test
        void queryRepository로_위임해서_결과를_그대로_반환한다() {
            UUID sourceId = UUID.randomUUID();
            CollectSourcePost p = newCollectSourcePost(IndexingStatus.INDEXED);
            Mockito.doReturn(List.of(p)).when(queryRepository).fetchReindexTargets(sourceId);

            List<CollectSourcePost> result = collectSourcePostService.getReindexTargetCollectSourcePosts(sourceId);

            Assertions.assertThat(result).containsExactly(p);
        }
    }

    @DisplayName("applyIndexResult 테스트")
    @Nested
    class ApplyIndexResultTest {

        @Test
        void 실패하지_않은_id는_INDEXED로_마킹된다() {
            IndexingJob job = runningIndexingJob(IndexingJobType.CRON);
            CollectSourcePost p1 = newCollectSourcePost(IndexingStatus.INDEXING);
            BulkOperationResult result = new BulkOperationResult(Set.of(), 1);
            OffsetDateTime now = OffsetDateTime.now();

            Mockito.doReturn(List.of(p1)).when(collectSourcePostRepository).findAllById(List.of(p1.id()));

            collectSourcePostService.applyIndexResult(List.of(p1.id()), result, job, now);

            Assertions.assertThat(p1.indexingStatus()).isEqualTo(IndexingStatus.INDEXED);
            Assertions.assertThat(p1.lastIndexedAt()).isEqualTo(now);
            Assertions.assertThat(p1.lastIndexingJob().id()).isEqualTo(job.id());
        }

        @Test
        void 실패한_id는_FAILED로_마킹되고_indexingErrorCount가_증가한다() {
            IndexingJob job = runningIndexingJob(IndexingJobType.CRON);
            CollectSourcePost p1 = newCollectSourcePost(IndexingStatus.INDEXING);
            BulkOperationResult result = new BulkOperationResult(Set.of(p1.id()), 0);

            Mockito.doReturn(List.of(p1)).when(collectSourcePostRepository).findAllById(List.of(p1.id()));

            collectSourcePostService.applyIndexResult(List.of(p1.id()), result, job, OffsetDateTime.now());

            Assertions.assertThat(p1.indexingStatus()).isEqualTo(IndexingStatus.FAILED);
            Assertions.assertThat(p1.indexingErrorCount()).isEqualTo(1);
        }

        @Test
        void 성공과_실패가_섞여있으면_각각_INDEXED와_FAILED로_마킹된다() {
            IndexingJob job = runningIndexingJob(IndexingJobType.CRON);
            CollectSourcePost ok = newCollectSourcePost(IndexingStatus.INDEXING);
            CollectSourcePost ng = newCollectSourcePost(IndexingStatus.INDEXING);
            BulkOperationResult result = new BulkOperationResult(Set.of(ng.id()), 1);
            OffsetDateTime now = OffsetDateTime.now();

            Mockito.doReturn(List.of(ok, ng))
                   .when(collectSourcePostRepository).findAllById(List.of(ok.id(), ng.id()));

            collectSourcePostService.applyIndexResult(List.of(ok.id(), ng.id()), result, job, now);

            Assertions.assertThat(ok.indexingStatus()).isEqualTo(IndexingStatus.INDEXED);
            Assertions.assertThat(ng.indexingStatus()).isEqualTo(IndexingStatus.FAILED);
        }

        @Test
        void findAllById_결과가_targetIds보다_작아도_예외없이_있는_것만_마킹한다() {
            // 중간에 누가 삭제한 경우. log.warn은 검증하지 않고 동작만 확인.
            IndexingJob job = runningIndexingJob(IndexingJobType.CRON);
            CollectSourcePost found = newCollectSourcePost(IndexingStatus.INDEXING);
            UUID missingId = UUID.randomUUID();
            BulkOperationResult result = new BulkOperationResult(Set.of(), 2);

            Mockito.doReturn(List.of(found))
                   .when(collectSourcePostRepository).findAllById(List.of(found.id(), missingId));

            Assertions.assertThatCode(() ->
                collectSourcePostService.applyIndexResult(List.of(found.id(), missingId),
                                                           result, job, OffsetDateTime.now())
            ).doesNotThrowAnyException();

            Assertions.assertThat(found.indexingStatus()).isEqualTo(IndexingStatus.INDEXED);
        }

        @Test
        void 같은_입력으로_다시_호출해도_같은_상태가_되어_멱등하다() {
            // 옵션B의 핵심: TX2 재시도 안전성.
            IndexingJob job = runningIndexingJob(IndexingJobType.CRON);
            CollectSourcePost p = newCollectSourcePost(IndexingStatus.INDEXING);
            BulkOperationResult result = new BulkOperationResult(Set.of(), 1);
            OffsetDateTime now = OffsetDateTime.now();

            Mockito.doReturn(List.of(p)).when(collectSourcePostRepository).findAllById(List.of(p.id()));

            collectSourcePostService.applyIndexResult(List.of(p.id()), result, job, now);
            IndexingStatus afterFirst = p.indexingStatus();

            collectSourcePostService.applyIndexResult(List.of(p.id()), result, job, now);
            IndexingStatus afterSecond = p.indexingStatus();

            Assertions.assertThat(afterFirst).isEqualTo(IndexingStatus.INDEXED);
            Assertions.assertThat(afterSecond).isEqualTo(IndexingStatus.INDEXED);
        }
    }

    @DisplayName("markIndexingBatch 테스트")
    @Nested
    class MarkIndexingBatchTest {

        @Test
        void targetSource만_있으면_재색인_대상_전체를_INDEXING으로_마킹한다() {
            IndexingJob job = IndexingJob.builder()
                                         .id(UUID.randomUUID())
                                         .indexingJobType(IndexingJobType.MANUAL)
                                         .jobStatus(JobStatus.RUNNING)
                                         .targetSource(collectSource)
                                         .totalCount(0)
                                         .indexedCount(0)
                                         .build();

            CollectSourcePost p1 = newCollectSourcePost(IndexingStatus.INDEXED);
            CollectSourcePost p2 = newCollectSourcePost(IndexingStatus.FAILED);
            Mockito.doReturn(List.of(p1, p2)).when(queryRepository).fetchReindexTargets(collectSource.id());

            List<CollectSourcePost> result = collectSourcePostService.markIndexingBatch(job);

            Assertions.assertThat(result).containsExactly(p1, p2);
            Assertions.assertThat(p1.indexingStatus()).isEqualTo(IndexingStatus.INDEXING);
            Assertions.assertThat(p2.indexingStatus()).isEqualTo(IndexingStatus.INDEXING);
            Assertions.assertThat(p1.lastIndexingJob().id()).isEqualTo(job.id());
            Assertions.assertThat(p2.lastIndexingJob().id()).isEqualTo(job.id());
        }

        @Test
        void targetPost만_있으면_id로_재조회해서_INDEXING으로_마킹한다() {
            CollectSourcePost target = newCollectSourcePost(IndexingStatus.INDEXED);
            IndexingJob job = IndexingJob.builder()
                                         .id(UUID.randomUUID())
                                         .indexingJobType(IndexingJobType.MANUAL)
                                         .jobStatus(JobStatus.RUNNING)
                                         .targetPost(target)
                                         .totalCount(0)
                                         .indexedCount(0)
                                         .build();

            // resolveManualTargets가 getCollectSourcePost(targetPost.id())로 재조회한다
            Mockito.doReturn(Optional.of(target)).when(queryRepository).fetchOneById(target.id());

            List<CollectSourcePost> result = collectSourcePostService.markIndexingBatch(job);

            Assertions.assertThat(result).containsExactly(target);
            Assertions.assertThat(target.indexingStatus()).isEqualTo(IndexingStatus.INDEXING);
            Assertions.assertThat(target.lastIndexingJob().id()).isEqualTo(job.id());

            // targetSource 경로는 호출되지 않아야 함
            Mockito.verify(queryRepository, Mockito.never()).fetchReindexTargets(any());
        }

        @Test
        void targetSource와_targetPost가_모두_null이면_IllegalStateException() {
            IndexingJob job = IndexingJob.builder()
                                         .id(UUID.randomUUID())
                                         .indexingJobType(IndexingJobType.MANUAL)
                                         .jobStatus(JobStatus.PENDING)
                                         .totalCount(0)
                                         .indexedCount(0)
                                         .build();

            Assertions.assertThatThrownBy(() -> collectSourcePostService.markIndexingBatch(job))
                      .isInstanceOf(IllegalStateException.class)
                      .hasMessageContaining("MANUAL job 대상이 잘못됨");
        }

        @Test
        void targetSource와_targetPost가_모두_있으면_IllegalStateException() {
            CollectSourcePost target = newCollectSourcePost(IndexingStatus.INDEXED);
            IndexingJob job = IndexingJob.builder()
                                         .id(UUID.randomUUID())
                                         .indexingJobType(IndexingJobType.MANUAL)
                                         .jobStatus(JobStatus.PENDING)
                                         .targetSource(collectSource)
                                         .targetPost(target)
                                         .totalCount(0)
                                         .indexedCount(0)
                                         .build();

            Assertions.assertThatThrownBy(() -> collectSourcePostService.markIndexingBatch(job))
                      .isInstanceOf(IllegalStateException.class)
                      .hasMessageContaining("MANUAL job 대상이 잘못됨");
        }
    }

    @DisplayName("recoverStaleIndexing 테스트")
    @Nested
    class RecoverStaleIndexingTest {

        @Test
        void stale_post들을_PENDING으로_되돌리고_복구_개수를_반환한다() {
            OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(15);
            CollectSourcePost stale1 = newCollectSourcePost(IndexingStatus.INDEXING);
            CollectSourcePost stale2 = newCollectSourcePost(IndexingStatus.INDEXING);

            Mockito.doReturn(List.of(stale1, stale2))
                   .when(queryRepository).fetchStaleIndexingTargets(eq(threshold), anyInt());

            int recovered = collectSourcePostService.recoverStaleIndexing(threshold, 100);

            Assertions.assertThat(recovered).isEqualTo(2);
            Assertions.assertThat(stale1.indexingStatus()).isEqualTo(IndexingStatus.PENDING);
            Assertions.assertThat(stale2.indexingStatus()).isEqualTo(IndexingStatus.PENDING);
            Assertions.assertThat(stale1.indexingErrorCount()).isZero();
            Assertions.assertThat(stale2.indexingErrorCount()).isZero();
        }

        @Test
        void stale이_없으면_0을_반환한다() {
            OffsetDateTime threshold = OffsetDateTime.now().minusMinutes(15);
            Mockito.doReturn(List.of())
                   .when(queryRepository).fetchStaleIndexingTargets(eq(threshold), anyInt());

            int recovered = collectSourcePostService.recoverStaleIndexing(threshold, 100);

            Assertions.assertThat(recovered).isZero();
        }
    }
}
