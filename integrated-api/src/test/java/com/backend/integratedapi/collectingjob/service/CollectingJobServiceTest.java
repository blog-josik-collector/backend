package com.backend.integratedapi.collectingjob.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.common.enums.ScheduleType;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedapi.collectingjob.repository.CollectingJobQueryRepository;
import com.backend.integratedapi.collectingjob.repository.CollectingJobRepository;
import com.backend.integratedapi.collectingjob.service.dto.CollectingJobDto;
import com.backend.integratedapi.collectsource.service.CollectSourceService;
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

@DisplayName("CollectingJobService 테스트")
@ExtendWith(MockitoExtension.class)
class CollectingJobServiceTest {

    @Spy
    @InjectMocks
    private CollectingJobService collectingJobService;

    @Mock
    private CollectingJobRepository collectingJobRepository;

    @Mock
    private CollectingJobQueryRepository queryRepository;

    @Mock
    private CollectSourceService collectSourceService;

    private PostProvider mockPostProvider;
    private CollectSource cronCollectSource;
    private CollectSource manualCollectSource;

    @BeforeEach
    void init() {
        mockPostProvider = PostProvider.builder()
                                       .id(UUID.randomUUID())
                                       .name("test_provider")
                                       .baseUrl("https://test.com")
                                       .description("test_description")
                                       .isUsed(true)
                                       .collectSources(new ArrayList<>())
                                       .build();

        cronCollectSource = CollectSource.builder()
                                         .id(UUID.randomUUID())
                                         .postProvider(mockPostProvider)
                                         .url("https://test.com/blog/1")
                                         .scheduleType(ScheduleType.CRON)
                                         .cronExpression("0 0 * * * *")
                                         .isUsed(false)
                                         .build();

        manualCollectSource = CollectSource.builder()
                                           .id(UUID.randomUUID())
                                           .postProvider(mockPostProvider)
                                           .url("https://test.com/blog/2")
                                           .scheduleType(ScheduleType.MANUAL)
                                           .isUsed(true)
                                           .build();
    }

    @DisplayName("CollectingJob 시작 테스트")
    @Nested
    class StartCollectingJobTest {

        @Test
        void CRON_타입_source로_CollectingJob을_시작하면_source가_활성화된다() {
            // given
            UUID userId = UUID.randomUUID();
            CollectingJobDto request = CollectingJobDto.of(cronCollectSource.id(), userId, 1, 5);

            CollectingJob savedJob = CollectingJob.builder()
                                                  .id(UUID.randomUUID())
                                                  .collectSource(cronCollectSource)
                                                  .jobStatus(JobStatus.PENDING)
                                                  .fromPage(1)
                                                  .toPage(5)
                                                  .build();

            Mockito.doReturn(cronCollectSource).when(collectSourceService).getCollectSource(any());
            Mockito.doReturn(savedJob).when(collectingJobRepository).save(any());

            Assertions.assertThat(cronCollectSource.isUsed()).isFalse();

            // when
            CollectingJobDto result = collectingJobService.start(request);

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(savedJob.id());
            Assertions.assertThat(result.jobStatus()).isEqualTo(JobStatus.PENDING);
            Assertions.assertThat(cronCollectSource.isUsed()).isTrue();
        }

        @Test
        void MANUAL_타입_source로_CollectingJob을_시작할_수_있다() {
            // given
            UUID userId = UUID.randomUUID();
            CollectingJobDto request = CollectingJobDto.of(manualCollectSource.id(), userId, 1, 5);

            CollectingJob savedJob = CollectingJob.builder()
                                                  .id(UUID.randomUUID())
                                                  .collectSource(manualCollectSource)
                                                  .jobStatus(JobStatus.PENDING)
                                                  .fromPage(1)
                                                  .toPage(5)
                                                  .triggeredBy(userId)
                                                  .build();

            Mockito.doReturn(manualCollectSource).when(collectSourceService).getCollectSource(any());
            Mockito.doReturn(Boolean.FALSE).when(queryRepository).existsActiveJob(any());
            Mockito.doReturn(savedJob).when(collectingJobRepository).save(any());

            // when
            CollectingJobDto result = collectingJobService.start(request);

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(savedJob.id());
            Assertions.assertThat(result.jobStatus()).isEqualTo(JobStatus.PENDING);
            Assertions.assertThat(result.triggeredBy()).isEqualTo(userId);
            Assertions.assertThat(result.fromPage()).isEqualTo(1);
            Assertions.assertThat(result.toPage()).isEqualTo(5);
        }

        @Test
        void MANUAL_타입_source가_비활성화_상태이면_CollectingJob_시작에_실패한다() {
            // given
            CollectSource inactiveSource = CollectSource.builder()
                                                       .id(UUID.randomUUID())
                                                       .postProvider(mockPostProvider)
                                                       .url("https://test.com/blog/3")
                                                       .scheduleType(ScheduleType.MANUAL)
                                                       .isUsed(false)
                                                       .build();

            UUID userId = UUID.randomUUID();
            CollectingJobDto request = CollectingJobDto.of(inactiveSource.id(), userId, 1, 5);

            Mockito.doReturn(inactiveSource).when(collectSourceService).getCollectSource(any());

            // when & then
            Assertions.assertThatThrownBy(() -> collectingJobService.start(request))
                      .isInstanceOf(IllegalStateException.class)
                      .hasMessageContaining("비활성 source는 실행할 수 없음");
        }

        @Test
        void MANUAL_타입_source에_이미_진행_중인_Job이_있으면_CollectingJob_시작에_실패한다() {
            // given
            UUID userId = UUID.randomUUID();
            CollectingJobDto request = CollectingJobDto.of(manualCollectSource.id(), userId, 1, 5);

            Mockito.doReturn(manualCollectSource).when(collectSourceService).getCollectSource(any());
            Mockito.doReturn(Boolean.TRUE).when(queryRepository).existsActiveJob(any());

            // when & then
            Assertions.assertThatThrownBy(() -> collectingJobService.start(request))
                      .isInstanceOf(IllegalStateException.class)
                      .hasMessageContaining("이미 진행 중인 Job 있음");
        }
    }

    @DisplayName("CollectingJob 중지 테스트")
    @Nested
    class StopCollectingJobTest {

        @Test
        void CRON_타입_source를_중지하면_source가_비활성화된다() {
            // given
            cronCollectSource.activate();
            Mockito.doReturn(cronCollectSource).when(collectSourceService).getCollectSource(any());

            Assertions.assertThat(cronCollectSource.isUsed()).isTrue();

            // when
            collectingJobService.stop(cronCollectSource.id());

            // then
            Assertions.assertThat(cronCollectSource.isUsed()).isFalse();
        }

        @Test
        void MANUAL_타입_source를_중지하면_상태가_변경되지_않는다() {
            // given
            Mockito.doReturn(manualCollectSource).when(collectSourceService).getCollectSource(any());

            Assertions.assertThat(manualCollectSource.isUsed()).isTrue();

            // when
            collectingJobService.stop(manualCollectSource.id());

            // then
            Assertions.assertThat(manualCollectSource.isUsed()).isTrue();
        }
    }

    @DisplayName("CollectingJob 조회 테스트")
    @Nested
    class ReadCollectingJobTest {

        @Test
        void page와_size를_입력하면_CollectingJob_목록을_조회할_수_있다() {
            // given
            int page = 0;
            int size = 10;

            CollectingJob collectingJob = CollectingJob.builder()
                                                       .id(UUID.randomUUID())
                                                       .collectSource(cronCollectSource)
                                                       .jobStatus(JobStatus.PENDING)
                                                       .fromPage(1)
                                                       .toPage(5)
                                                       .build();

            OffsetPageResult<CollectingJob> pageResult = new OffsetPageResult<>(1L, page, size, List.of(collectingJob));

            Mockito.doReturn(pageResult).when(queryRepository).fetchCollectingJobs(page, size);

            // when
            OffsetPageResult<CollectingJobDto> result = collectingJobService.getCollectingJobs(page, size);

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getTotalCount()).isEqualTo(1L);
            Assertions.assertThat(result.getItems()).hasSize(1);
            Assertions.assertThat(result.getItems().get(0).id()).isEqualTo(collectingJob.id());
        }

        @Test
        void id를_입력하면_CollectingJobDto를_조회할_수_있다() {
            // given
            CollectingJob collectingJob = CollectingJob.builder()
                                                       .id(UUID.randomUUID())
                                                       .collectSource(cronCollectSource)
                                                       .jobStatus(JobStatus.PENDING)
                                                       .fromPage(1)
                                                       .toPage(5)
                                                       .build();

            Mockito.doReturn(java.util.Optional.of(collectingJob)).when(queryRepository).fetchOneById(any());

            // when
            CollectingJobDto result = collectingJobService.getCollectingJobDto(collectingJob.id());

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(collectingJob.id());
            Assertions.assertThat(result.jobStatus()).isEqualTo(JobStatus.PENDING);
        }

        @Test
        void id를_입력하면_CollectingJob_엔티티를_조회할_수_있다() {
            // given
            CollectingJob collectingJob = CollectingJob.builder()
                                                       .id(UUID.randomUUID())
                                                       .collectSource(cronCollectSource)
                                                       .jobStatus(JobStatus.PENDING)
                                                       .build();

            Mockito.doReturn(java.util.Optional.of(collectingJob)).when(queryRepository).fetchOneById(any());

            // when
            CollectingJob result = collectingJobService.getCollectingJob(collectingJob.id());

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(collectingJob.id());
        }

        @Test
        void id가_null이면_CollectingJob_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> collectingJobService.getCollectingJob(null))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }

        @Test
        void 존재하지_않는_id로_조회하면_CollectingJob_조회에_실패한다() {
            // given
            UUID id = UUID.randomUUID();
            Mockito.doReturn(java.util.Optional.empty()).when(queryRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> collectingJobService.getCollectingJob(id))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("존재하지 않는 collectJob입니다.");
        }
    }
}
