package com.backend.integratedapi.collectsource.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.enums.ScheduleType;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedapi.collectsource.repository.CollectSourceQueryRepository;
import com.backend.integratedapi.collectsource.repository.CollectSourceRepository;
import com.backend.integratedapi.collectsource.service.dto.CollectSourceDto;
import com.backend.integratedapi.provider.service.PostProviderService;
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

@DisplayName("CollectSourceService 테스트")
@ExtendWith(MockitoExtension.class)
class CollectSourceServiceTest {

    @Spy
    @InjectMocks
    private CollectSourceService collectSourceService;

    @Mock
    private CollectSourceRepository collectSourceRepository;

    @Mock
    private CollectSourceQueryRepository queryRepository;

    @Mock
    private PostProviderService postProviderService;

    private PostProvider mockPostProvider;
    private CollectSource mockCollectSource;

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

        mockCollectSource = CollectSource.builder()
                                         .id(UUID.randomUUID())
                                         .postProvider(mockPostProvider)
                                         .url("https://test.com/blog/1")
                                         .scheduleType(ScheduleType.CRON)
                                         .cronExpression("0 0 * * * *")
                                         .isUsed(true)
                                         .build();
    }

    @DisplayName("CollectSource 생성 테스트")
    @Nested
    class CreateCollectSourceTest {

        @Test
        void CRON_스케줄_타입으로_CollectSource를_생성할_수_있다() {
            // given
            CollectSourceDto request = CollectSourceDto.of(mockPostProvider.id(),
                                                          "https://test.com/blog/1",
                                                          ScheduleType.CRON,
                                                          "0 0 * * * *");

            Mockito.doReturn(mockPostProvider).when(postProviderService).getPostProvider(any());
            Mockito.doReturn(mockCollectSource).when(collectSourceRepository).save(any());

            // when
            CollectSourceDto created = collectSourceService.create(request);

            // then
            Assertions.assertThat(created).isNotNull();
            Assertions.assertThat(created.id()).isEqualTo(mockCollectSource.id());
            Assertions.assertThat(created.providerId()).isEqualTo(mockPostProvider.id());
            Assertions.assertThat(created.scheduleType()).isEqualTo(ScheduleType.CRON);
            Assertions.assertThat(created.cronExpression()).isEqualTo("0 0 * * * *");
            Assertions.assertThat(created.isUsed()).isTrue();
        }

        @Test
        void MANUAL_스케줄_타입으로_CollectSource를_생성할_수_있다() {
            // given
            CollectSource manualCollectSource = CollectSource.builder()
                                                             .id(UUID.randomUUID())
                                                             .postProvider(mockPostProvider)
                                                             .url("https://test.com/blog/1")
                                                             .scheduleType(ScheduleType.MANUAL)
                                                             .isUsed(true)
                                                             .build();

            CollectSourceDto request = CollectSourceDto.of(mockPostProvider.id(),
                                                          "https://test.com/blog/1",
                                                          ScheduleType.MANUAL,
                                                          null);

            Mockito.doReturn(mockPostProvider).when(postProviderService).getPostProvider(any());
            Mockito.doReturn(manualCollectSource).when(collectSourceRepository).save(any());

            // when
            CollectSourceDto created = collectSourceService.create(request);

            // then
            Assertions.assertThat(created).isNotNull();
            Assertions.assertThat(created.scheduleType()).isEqualTo(ScheduleType.MANUAL);
            Assertions.assertThat(created.cronExpression()).isNull();
        }

        @Test
        void providerId가_null이면_CollectSource_생성에_실패한다() {
            // given
            CollectSourceDto request = CollectSourceDto.of(null,
                                                          "https://test.com/blog/1",
                                                          ScheduleType.CRON,
                                                          "0 0 * * * *");

            // when & then
            Assertions.assertThatThrownBy(() -> collectSourceService.create(request))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("provider_id는 필수 입력값입니다.");
        }

        @Test
        void scheduleType이_null이면_CollectSource_생성에_실패한다() {
            // given
            CollectSourceDto request = CollectSourceDto.of(mockPostProvider.id(),
                                                          "https://test.com/blog/1",
                                                          null,
                                                          null);

            // when & then
            Assertions.assertThatThrownBy(() -> collectSourceService.create(request))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("schedule_type은 필수 입력값입니다.");
        }

        @Test
        void scheduleType이_MANUAL인데_cronExpression이_있으면_CollectSource_생성에_실패한다() {
            // given
            CollectSourceDto request = CollectSourceDto.of(mockPostProvider.id(),
                                                          "https://test.com/blog/1",
                                                          ScheduleType.MANUAL,
                                                          "0 0 * * * *");

            // when & then
            Assertions.assertThatThrownBy(() -> collectSourceService.create(request))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("schedule_type이 manual일 때 cron_expression은 입력할 수 없습니다.");
        }

        @Test
        void scheduleType이_CRON인데_cronExpression이_없으면_CollectSource_생성에_실패한다() {
            // given
            CollectSourceDto request = CollectSourceDto.of(mockPostProvider.id(),
                                                          "https://test.com/blog/1",
                                                          ScheduleType.CRON,
                                                          null);

            // when & then
            Assertions.assertThatThrownBy(() -> collectSourceService.create(request))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("schedule_type이 cron일 때 cron_expression은 필수입니다.");
        }
    }

    @DisplayName("CollectSource 조회 테스트")
    @Nested
    class ReadCollectSourceTest {

        @Test
        void page와_size를_입력하면_CollectSource_목록을_조회할_수_있다() {
            // given
            int page = 0;
            int size = 10;
            OffsetPageResult<CollectSource> pageResult = new OffsetPageResult<>(1L, page, size, List.of(mockCollectSource));

            Mockito.doReturn(pageResult).when(queryRepository).fetchCollectSources(page, size);

            // when
            OffsetPageResult<CollectSourceDto> result = collectSourceService.getCollectSources(page, size);

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getTotalCount()).isEqualTo(1L);
            Assertions.assertThat(result.getItems()).hasSize(1);
            Assertions.assertThat(result.getItems().get(0).id()).isEqualTo(mockCollectSource.id());
        }

        @Test
        void id를_입력하면_CollectSourceDto를_조회할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockCollectSource)).when(queryRepository).fetchOneById(any());

            // when
            CollectSourceDto result = collectSourceService.getCollectSourceDto(mockCollectSource.id());

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(mockCollectSource.id());
            Assertions.assertThat(result.providerId()).isEqualTo(mockPostProvider.id());
        }

        @Test
        void id를_입력하면_CollectSource_엔티티를_조회할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockCollectSource)).when(queryRepository).fetchOneById(any());

            // when
            CollectSource result = collectSourceService.getCollectSource(mockCollectSource.id());

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(mockCollectSource.id());
        }

        @Test
        void id가_null이면_CollectSource_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> collectSourceService.getCollectSourceDto(null))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }

        @Test
        void 존재하지_않는_id로_조회하면_CollectSource_조회에_실패한다() {
            // given
            UUID id = UUID.randomUUID();
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> collectSourceService.getCollectSource(id))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("존재하지 않는 collectSource입니다.");
        }
    }

    @DisplayName("CollectSource 수정 테스트")
    @Nested
    class UpdateCollectSourceTest {

        @Test
        void url을_업데이트할_수_있다() {
            // given
            String newUrl = "https://test.com/blog/updated";
            CollectSourceDto request = CollectSourceDto.of(mockCollectSource.id(), newUrl, null, null, true);

            Mockito.doReturn(Optional.of(mockCollectSource)).when(queryRepository).fetchOneById(any());

            // when
            collectSourceService.update(request);

            // then
            Assertions.assertThat(mockCollectSource.url()).isEqualTo(newUrl);
        }

        @Test
        void cronExpression을_업데이트할_수_있다() {
            // given
            String newCron = "0 30 * * * *";
            CollectSourceDto request = CollectSourceDto.of(mockCollectSource.id(), null, null, newCron, true);

            Mockito.doReturn(Optional.of(mockCollectSource)).when(queryRepository).fetchOneById(any());

            // when
            collectSourceService.update(request);

            // then
            Assertions.assertThat(mockCollectSource.cronExpression()).isEqualTo(newCron);
        }

        @Test
        void scheduleType을_MANUAL로_변경하면_cronExpression이_초기화된다() {
            // given
            CollectSourceDto request = CollectSourceDto.of(mockCollectSource.id(),
                                                          null,
                                                          ScheduleType.MANUAL,
                                                          null,
                                                          true);

            Mockito.doReturn(Optional.of(mockCollectSource)).when(queryRepository).fetchOneById(any());

            Assertions.assertThat(mockCollectSource.cronExpression()).isNotBlank();

            // when
            collectSourceService.update(request);

            // then
            Assertions.assertThat(mockCollectSource.scheduleType()).isEqualTo(ScheduleType.MANUAL);
            Assertions.assertThat(mockCollectSource.cronExpression()).isEmpty();
        }

        @Test
        void scheduleType을_CRON으로_변경하면서_cronExpression을_지정할_수_있다() {
            // given
            CollectSource manualSource = CollectSource.builder()
                                                     .id(UUID.randomUUID())
                                                     .postProvider(mockPostProvider)
                                                     .url("https://test.com/blog/1")
                                                     .scheduleType(ScheduleType.MANUAL)
                                                     .isUsed(true)
                                                     .build();

            String newCron = "0 0 * * * *";
            CollectSourceDto request = CollectSourceDto.of(manualSource.id(),
                                                          null,
                                                          ScheduleType.CRON,
                                                          newCron,
                                                          true);

            Mockito.doReturn(Optional.of(manualSource)).when(queryRepository).fetchOneById(any());

            // when
            collectSourceService.update(request);

            // then
            Assertions.assertThat(manualSource.scheduleType()).isEqualTo(ScheduleType.CRON);
            Assertions.assertThat(manualSource.cronExpression()).isEqualTo(newCron);
        }

        @Test
        void id가_null이면_수정에_실패한다() {
            // given
            CollectSourceDto request = CollectSourceDto.of(null, "https://test.com", ScheduleType.CRON, "0 0 * * * *", true);

            // when & then
            Assertions.assertThatThrownBy(() -> collectSourceService.update(request))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }

        @Test
        void scheduleType과_cronExpression의_조합이_올바르지_않으면_수정에_실패한다() {
            // given
            CollectSourceDto request = CollectSourceDto.of(mockCollectSource.id(),
                                                          null,
                                                          ScheduleType.CRON,
                                                          null,
                                                          true);

            Mockito.doReturn(Optional.of(mockCollectSource)).when(queryRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> collectSourceService.update(request))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("schedule_type이 cron일 때 cron_expression은 필수입니다.");
        }
    }

    @DisplayName("CollectSource 삭제 테스트")
    @Nested
    class DeleteCollectSourceTest {

        @Test
        void id를_입력하면_CollectSource를_삭제할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockCollectSource)).when(queryRepository).fetchOneById(any());

            // when
            collectSourceService.delete(mockCollectSource.id());

            // then
            Assertions.assertThat(mockCollectSource.isUsed()).isFalse();
            Assertions.assertThat(mockCollectSource.deletedAt()).isNotNull();
            Assertions.assertThat(mockCollectSource.isDelete()).isTrue();
        }

        @Test
        void id가_null이면_삭제에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> collectSourceService.delete(null))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }
    }
}
