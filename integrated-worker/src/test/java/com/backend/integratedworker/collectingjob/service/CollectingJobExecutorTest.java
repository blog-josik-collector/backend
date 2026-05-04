package com.backend.integratedworker.collectingjob.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.common.enums.ScheduleType;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectingjob.repository.CollectingJobQueryRepository;
import com.backend.integratedworker.collectingjob.service.crawler.BlogCrawlerService;
import com.backend.integratedworker.collectingjob.service.crawler.kakao.KakaoPost;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import com.backend.integratedworker.collectsourcepost.service.CollectSourcePostService;
import java.time.LocalDate;
import java.util.ArrayList;
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
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("CollectingJobExecutor 테스트")
@ExtendWith(MockitoExtension.class)
class CollectingJobExecutorTest {

    @Spy
    @InjectMocks
    private CollectingJobExecutor collectingJobExecutor;

    @Mock
    private CollectingJobQueryRepository queryRepository;

    @Mock
    private BlogCrawlerService blogCrawlerService;

    @Mock
    private CollectSourcePostService collectSourcePostService;

    private CollectSource collectSource;
    private CollectingJob collectingJob;

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
                                     .scheduleType(ScheduleType.MANUAL)
                                     .isUsed(true)
                                     .build();

        collectingJob = CollectingJob.builder()
                                     .id(UUID.randomUUID())
                                     .collectSource(collectSource)
                                     .jobStatus(JobStatus.RUNNING)
                                     .fromPage(1)
                                     .toPage(1)
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

    @DisplayName("CollectingJob 비동기 실행 테스트")
    @Nested
    class ExecuteAsyncTest {

        @Test
        void 새로운_Post는_생성_후_Job을_SUCCESS로_마킹한다() {
            // given
            Post post = newPost("https://test.com/blog/1/post/new");

            Mockito.doReturn(Optional.of(collectingJob)).when(queryRepository).fetchOneById(any());
            Mockito.doReturn(List.of(post)).when(blogCrawlerService).fetch(any());
            Mockito.doReturn(null).when(collectSourcePostService).getCollectSourcePost(anyString());

            // when
            collectingJobExecutor.executeAsync(collectingJob.id());

            // then
            Mockito.verify(collectSourcePostService).create(eq(post), eq(collectSource), eq(collectingJob));
            Mockito.verify(collectSourcePostService, Mockito.never()).update(any(), any(), any());
            Assertions.assertThat(collectingJob.jobStatus()).isEqualTo(JobStatus.SUCCESS);
            Assertions.assertThat(collectingJob.endedAt()).isNotNull();
            Assertions.assertThat(collectingJob.totalCount()).isEqualTo(1);
            Assertions.assertThat(collectingJob.collectedCount()).isEqualTo(1);
        }

        @Test
        void 기존_Post와_콘텐츠_해시가_같으면_lastCollect만_갱신하고_Job을_SUCCESS로_마킹한다() {
            // given
            Post post = newPost("https://test.com/blog/1/post/exists");
            String sameHash = "same_hash_value";

            CollectSourcePost existing = CollectSourcePost.builder()
                                                          .id(UUID.randomUUID())
                                                          .collectSource(collectSource)
                                                          .url(post.getUrl())
                                                          .title("title")
                                                          .contentHash(sameHash)
                                                          .lastCollectingJob(collectingJob)
                                                          .build();

            Mockito.doReturn(Optional.of(collectingJob)).when(queryRepository).fetchOneById(any());
            Mockito.doReturn(List.of(post)).when(blogCrawlerService).fetch(any());
            Mockito.doReturn(existing).when(collectSourcePostService).getCollectSourcePost(anyString());
            Mockito.doReturn(sameHash).when(collectSourcePostService).createContentHash(any());

            // when
            collectingJobExecutor.executeAsync(collectingJob.id());

            // then
            Mockito.verify(collectSourcePostService, Mockito.never()).create(any(), any(), any());
            Mockito.verify(collectSourcePostService, Mockito.never()).update(any(), any(), any());
            Assertions.assertThat(existing.lastCollectedAt()).isNotNull();
            Assertions.assertThat(collectingJob.jobStatus()).isEqualTo(JobStatus.SUCCESS);
        }

        @Test
        void 기존_Post와_콘텐츠_해시가_다르면_update를_호출하고_Job을_SUCCESS로_마킹한다() {
            // given
            Post post = newPost("https://test.com/blog/1/post/changed");

            CollectSourcePost existing = CollectSourcePost.builder()
                                                          .id(UUID.randomUUID())
                                                          .collectSource(collectSource)
                                                          .url(post.getUrl())
                                                          .title("title")
                                                          .contentHash("old_hash")
                                                          .lastCollectingJob(collectingJob)
                                                          .build();

            Mockito.doReturn(Optional.of(collectingJob)).when(queryRepository).fetchOneById(any());
            Mockito.doReturn(List.of(post)).when(blogCrawlerService).fetch(any());
            Mockito.doReturn(existing).when(collectSourcePostService).getCollectSourcePost(anyString());
            Mockito.doReturn("new_hash").when(collectSourcePostService).createContentHash(any());

            // when
            collectingJobExecutor.executeAsync(collectingJob.id());

            // then
            Mockito.verify(collectSourcePostService).update(eq(existing.id()), eq(post), eq(collectingJob));
            Mockito.verify(collectSourcePostService, Mockito.never()).create(any(), any(), any());
            Assertions.assertThat(collectingJob.jobStatus()).isEqualTo(JobStatus.SUCCESS);
        }

        @Test
        void 크롤링_도중_예외가_발생하면_Job을_FAILED로_마킹한다() {
            // given
            String errorMessage = "crawler exploded";

            Mockito.doReturn(Optional.of(collectingJob)).when(queryRepository).fetchOneById(any());
            Mockito.doThrow(new RuntimeException(errorMessage)).when(blogCrawlerService).fetch(any());

            // when
            collectingJobExecutor.executeAsync(collectingJob.id());

            // then
            Assertions.assertThat(collectingJob.jobStatus()).isEqualTo(JobStatus.FAILED);
            Assertions.assertThat(collectingJob.endedAt()).isNotNull();
            Assertions.assertThat(collectingJob.errorMessage()).isEqualTo(errorMessage);
        }
    }
}
