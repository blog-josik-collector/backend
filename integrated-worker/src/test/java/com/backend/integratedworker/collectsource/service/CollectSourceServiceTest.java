package com.backend.integratedworker.collectsource.service;

import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.enums.ScheduleType;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectsource.repository.CollectSourceQueryRepository;
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

@DisplayName("(Worker) CollectSourceService 테스트")
@ExtendWith(MockitoExtension.class)
class CollectSourceServiceTest {

    @Spy
    @InjectMocks
    private CollectSourceService collectSourceService;

    @Mock
    private CollectSourceQueryRepository queryRepository;

    @DisplayName("CRON CollectSource 조회 테스트")
    @Nested
    class GetActiveCronCollectSourcesTest {

        @Test
        void 활성화된_CRON_CollectSource_목록을_조회할_수_있다() {
            // given
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
                                                       .scheduleType(ScheduleType.CRON)
                                                       .cronExpression("0 0 * * * *")
                                                       .isUsed(true)
                                                       .build();

            Mockito.doReturn(List.of(collectSource)).when(queryRepository).findActiveCronCollectSources();

            // when
            List<CollectSource> result = collectSourceService.getActiveCronCollectSources();

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).hasSize(1);
            Assertions.assertThat(result.get(0).id()).isEqualTo(collectSource.id());
            Assertions.assertThat(result.get(0).scheduleType()).isEqualTo(ScheduleType.CRON);
        }

        @Test
        void 활성화된_CRON_CollectSource가_없으면_빈_리스트를_반환한다() {
            // given
            Mockito.doReturn(List.of()).when(queryRepository).findActiveCronCollectSources();

            // when
            List<CollectSource> result = collectSourceService.getActiveCronCollectSources();

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).isEmpty();
        }
    }
}
