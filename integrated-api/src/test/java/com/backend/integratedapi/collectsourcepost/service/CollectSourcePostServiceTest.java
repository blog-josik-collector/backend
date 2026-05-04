package com.backend.integratedapi.collectsourcepost.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.common.enums.IndexingStatus;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.common.enums.ScheduleType;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedapi.collectsourcepost.repository.CollectSourcePostQueryRepository;
import com.backend.integratedapi.collectsourcepost.service.dto.CollectSourcePostDto;
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

@DisplayName("CollectSourcePostService 테스트")
@ExtendWith(MockitoExtension.class)
class CollectSourcePostServiceTest {

    @Spy
    @InjectMocks
    private CollectSourcePostService collectSourcePostService;

    @Mock
    private CollectSourcePostQueryRepository queryRepository;

    private CollectSourcePost mockCollectSourcePost;

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

        CollectSource collectSource = CollectSource.builder()
                                                   .id(UUID.randomUUID())
                                                   .postProvider(postProvider)
                                                   .url("https://test.com/blog/1")
                                                   .scheduleType(ScheduleType.MANUAL)
                                                   .isUsed(true)
                                                   .build();

        CollectingJob collectingJob = CollectingJob.builder()
                                                   .id(UUID.randomUUID())
                                                   .collectSource(collectSource)
                                                   .jobStatus(JobStatus.SUCCESS)
                                                   .build();

        mockCollectSourcePost = CollectSourcePost.builder()
                                                 .id(UUID.randomUUID())
                                                 .collectSource(collectSource)
                                                 .title("test_title")
                                                 .url("https://test.com/blog/1/post/1")
                                                 .publishedAt(LocalDate.now())
                                                 .thumbnailUrl("https://test.com/thumb.png")
                                                 .summary("test_summary")
                                                 .indexingStatus(IndexingStatus.INDEX_PENDING)
                                                 .indexingErrorCount(0)
                                                 .lastCollectingJob(collectingJob)
                                                 .build();
    }

    @DisplayName("CollectSourcePost 조회 테스트")
    @Nested
    class ReadCollectSourcePostTest {

        @Test
        void id를_입력하면_CollectSourcePostDto를_조회할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockCollectSourcePost)).when(queryRepository).fetchOneById(any());

            // when
            CollectSourcePostDto result = collectSourcePostService.getCollectSourcePostDto(mockCollectSourcePost.id());

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(mockCollectSourcePost.id());
            Assertions.assertThat(result.collectSourceId()).isEqualTo(mockCollectSourcePost.collectSource().id());
            Assertions.assertThat(result.title()).isEqualTo(mockCollectSourcePost.title());
            Assertions.assertThat(result.url()).isEqualTo(mockCollectSourcePost.url());
            Assertions.assertThat(result.lastCollectingJobId()).isEqualTo(mockCollectSourcePost.lastCollectingJob().id());
        }

        @Test
        void id를_입력하면_CollectSourcePost_엔티티를_조회할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockCollectSourcePost)).when(queryRepository).fetchOneById(any());

            // when
            CollectSourcePost result = collectSourcePostService.getCollectSourcePost(mockCollectSourcePost.id());

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(mockCollectSourcePost.id());
        }

        @Test
        void id가_null이면_CollectSourcePost_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> collectSourcePostService.getCollectSourcePostDto(null))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }

        @Test
        void 존재하지_않는_id로_조회하면_CollectSourcePost_조회에_실패한다() {
            // given
            UUID id = UUID.randomUUID();
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> collectSourcePostService.getCollectSourcePost(id))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("존재하지 않는 id입니다.");
        }
    }
}
