package com.backend.integratedworker.collectsourcepost.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.common.enums.ScheduleType;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectingjob.service.crawler.kakao.KakaoPost;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import com.backend.integratedworker.collectsourcepost.repository.CollectSourcePostQueryRepository;
import com.backend.integratedworker.collectsourcepost.repository.CollectSourcePostRepository;
import java.time.LocalDate;
import java.util.ArrayList;
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
                                     .scheduleType(ScheduleType.MANUAL)
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
}
