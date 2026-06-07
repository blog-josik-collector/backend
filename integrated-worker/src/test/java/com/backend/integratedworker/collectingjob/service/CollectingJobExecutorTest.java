package com.backend.integratedworker.collectingjob.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.common.enums.CollectScheduleType;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectingjob.service.crawler.BlogCrawlerService;
import com.backend.integratedworker.collectingjob.service.crawler.kakao.KakaoPost;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("CollectingJobExecutor 테스트")
@ExtendWith(MockitoExtension.class)
class CollectingJobExecutorTest {

    @InjectMocks
    private CollectingJobExecutor collectingJobExecutor;

    @Mock
    private CollectingJobService collectingJobService;

    @Mock
    private BlogCrawlerService blogCrawlerService;

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
                                     .collectScheduleType(CollectScheduleType.MANUAL)
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
                        .build();
    }

    @DisplayName("CollectingJob 비동기 실행 테스트")
    @Nested
    class ExecuteAsyncTest {

        @Test
        void 크롤링_성공_시_finishCollect를_호출한다() {
            List<Post> posts = List.of(newPost("https://test.com/blog/1/post/new"));

            Mockito.doReturn(collectingJob).when(collectingJobService).getJobForExecution(collectingJob.id());
            Mockito.doReturn(posts).when(blogCrawlerService).fetch(collectingJob);

            collectingJobExecutor.executeAsync(collectingJob.id());

            Mockito.verify(collectingJobService).finishCollect(
                    eq(collectingJob.id()),
                    eq(collectSource.id()),
                    eq(false),
                    eq(posts),
                    any(OffsetDateTime.class));
            Mockito.verify(collectingJobService, Mockito.never()).markFailed(any(), any(), any());
        }

        @Test
        void 크롤링_도중_예외가_발생하면_Job을_FAILED로_마킹한다() {
            String errorMessage = "crawler exploded";

            Mockito.doReturn(collectingJob).when(collectingJobService).getJobForExecution(collectingJob.id());
            Mockito.doThrow(new RuntimeException(errorMessage)).when(blogCrawlerService).fetch(collectingJob);

            collectingJobExecutor.executeAsync(collectingJob.id());

            Mockito.verify(collectingJobService).markFailed(eq(collectingJob.id()), any(OffsetDateTime.class), eq(errorMessage));
            Mockito.verify(collectingJobService, Mockito.never()).finishCollect(any(), any(), anyBoolean(), anyList(), any());
        }
    }
}
