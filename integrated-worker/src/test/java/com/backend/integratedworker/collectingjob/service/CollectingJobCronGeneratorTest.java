package com.backend.integratedworker.collectingjob.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.enums.ScheduleType;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectingjob.repository.CollectingJobQueryRepository;
import com.backend.integratedworker.collectingjob.repository.CollectingJobRepository;
import com.backend.integratedworker.collectsource.service.CollectSourceService;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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

@DisplayName("CollectingJobCronGenerator 테스트")
@ExtendWith(MockitoExtension.class)
class CollectingJobCronGeneratorTest {

    @Spy
    @InjectMocks
    private CollectingJobCronGenerator collectingJobCronGenerator;

    @Mock
    private CollectingJobRepository collectingJobRepository;

    @Mock
    private CollectingJobQueryRepository queryRepository;

    @Mock
    private CollectSourceService collectSourceService;

    private PostProvider postProvider;

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
    }

    private CollectSource buildCronSource(String cronExpression) {
        return CollectSource.builder()
                            .id(UUID.randomUUID())
                            .postProvider(postProvider)
                            .url("https://test.com/blog/1")
                            .scheduleType(ScheduleType.CRON)
                            .cronExpression(cronExpression)
                            .isUsed(true)
                            .build();
    }

    @DisplayName("CollectingJob 생성 테스트")
    @Nested
    class GenerateCollectingJobTest {

        @Test
        void 활성_CRON_CollectSource가_없으면_CollectingJob을_생성하지_않는다() {
            // given
            Mockito.doReturn(List.of()).when(collectSourceService).getActiveCronCollectSources();

            // when
            collectingJobCronGenerator.generate();

            // then
            Mockito.verify(collectingJobRepository, Mockito.never()).save(any());
        }

        @Test
        void cron이_due_상태이고_active_job이_없으면_CollectingJob을_생성한다() {
            // given - 매초 실행되는 cron이므로 항상 due
            CollectSource source = buildCronSource("* * * * * *");

            Mockito.doReturn(List.of(source)).when(collectSourceService).getActiveCronCollectSources();
            Mockito.doReturn(Boolean.FALSE).when(queryRepository).existsActiveJob(source.id());
            Mockito.doReturn(CollectingJob.builder().id(UUID.randomUUID()).collectSource(source).build())
                   .when(collectingJobRepository).save(any());

            // when
            collectingJobCronGenerator.generate();

            // then
            Mockito.verify(collectingJobRepository, Mockito.times(1)).save(any());
        }

        @Test
        void cron이_due_상태이지만_active_job이_있으면_CollectingJob을_생성하지_않는다() {
            // given
            CollectSource source = buildCronSource("* * * * * *");

            Mockito.doReturn(List.of(source)).when(collectSourceService).getActiveCronCollectSources();
            Mockito.doReturn(Boolean.TRUE).when(queryRepository).existsActiveJob(source.id());

            // when
            collectingJobCronGenerator.generate();

            // then
            Mockito.verify(collectingJobRepository, Mockito.never()).save(any());
        }

        @Test
        void cron이_due_상태가_아니면_CollectingJob을_생성하지_않는다() {
            // given - 2월 29일 자정만 실행되는 cron(=현재 시점에서는 due 아님)
            CollectSource source = buildCronSource("0 0 0 29 2 *");

            Mockito.doReturn(List.of(source)).when(collectSourceService).getActiveCronCollectSources();

            // when
            collectingJobCronGenerator.generate();

            // then
            Mockito.verify(queryRepository, Mockito.never()).existsActiveJob(any());
            Mockito.verify(collectingJobRepository, Mockito.never()).save(any());
        }

        @Test
        void 잘못된_cron_표현식이_있으면_예외를_삼키고_다음_source로_넘어간다() {
            // given
            CollectSource invalidSource = buildCronSource("invalid_cron");
            CollectSource validSource = buildCronSource("* * * * * *");

            Mockito.doReturn(List.of(invalidSource, validSource))
                   .when(collectSourceService).getActiveCronCollectSources();
            Mockito.doReturn(Boolean.FALSE).when(queryRepository).existsActiveJob(validSource.id());
            Mockito.doReturn(CollectingJob.builder().id(UUID.randomUUID()).collectSource(validSource).build())
                   .when(collectingJobRepository).save(any());

            // when & then (예외가 propagate되지 않음)
            collectingJobCronGenerator.generate();

            Mockito.verify(collectingJobRepository, Mockito.times(1)).save(any());
        }
    }
}
