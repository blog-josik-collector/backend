package com.backend.integratedapi.provider.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedapi.provider.repository.PostProviderQueryRepository;
import com.backend.integratedapi.provider.repository.PostProviderRepository;
import com.backend.integratedapi.provider.service.dto.PostProviderDto;
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

@DisplayName("PostProviderService 테스트")
@ExtendWith(MockitoExtension.class)
class PostProviderServiceTest {

    @Spy
    @InjectMocks
    private PostProviderService postProviderService;

    @Mock
    private PostProviderRepository postProviderRepository;

    @Mock
    private PostProviderQueryRepository queryRepository;

    private PostProvider mockPostProvider;

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
    }

    @DisplayName("PostProvider 생성 테스트")
    @Nested
    class CreatePostProviderTest {

        @Test
        void postProviderDto를_입력하면_PostProvider를_생성할_수_있다() {
            // given
            PostProviderDto request = PostProviderDto.of(mockPostProvider.name(),
                                                        mockPostProvider.baseUrl(),
                                                        mockPostProvider.description());

            Mockito.doReturn(Boolean.FALSE).when(postProviderRepository).existsByName(any());
            Mockito.doReturn(mockPostProvider).when(postProviderRepository).save(any());

            // when
            PostProviderDto created = postProviderService.create(request);

            // then
            Assertions.assertThat(created).isNotNull();
            Assertions.assertThat(created.id()).isEqualTo(mockPostProvider.id());
            Assertions.assertThat(created.name()).isEqualTo(mockPostProvider.name());
            Assertions.assertThat(created.baseUrl()).isEqualTo(mockPostProvider.baseUrl());
            Assertions.assertThat(created.description()).isEqualTo(mockPostProvider.description());
            Assertions.assertThat(created.isUsed()).isTrue();
        }

        @Test
        void name이_빈값이면_PostProvider_생성에_실패한다() {
            // given
            PostProviderDto request = PostProviderDto.of(null, "https://test.com", "test_description");

            // when & then
            Assertions.assertThatThrownBy(() -> postProviderService.create(request))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("name은 필수 입력값입니다.");
        }

        @Test
        void baseUrl이_빈값이면_PostProvider_생성에_실패한다() {
            // given
            PostProviderDto request = PostProviderDto.of("test_provider", null, "test_description");

            // when & then
            Assertions.assertThatThrownBy(() -> postProviderService.create(request))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("baseUrl은 필수 입력값입니다.");
        }

        @Test
        void description이_빈값이면_PostProvider_생성에_실패한다() {
            // given
            PostProviderDto request = PostProviderDto.of("test_provider", "https://test.com", null);

            // when & then
            Assertions.assertThatThrownBy(() -> postProviderService.create(request))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("description은 필수 입력값입니다.");
        }

        @Test
        void name이_중복되면_PostProvider_생성에_실패한다() {
            // given
            PostProviderDto request = PostProviderDto.of(mockPostProvider.name(),
                                                        mockPostProvider.baseUrl(),
                                                        mockPostProvider.description());

            Mockito.doReturn(Boolean.TRUE).when(postProviderRepository).existsByName(any());

            // when & then
            Assertions.assertThatThrownBy(() -> postProviderService.create(request))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("이미 존재하는 postProvider입니다.");
        }
    }

    @DisplayName("PostProvider 조회 테스트")
    @Nested
    class ReadPostProviderTest {

        @Test
        void page와_size를_입력하면_PostProvider_목록을_조회할_수_있다() {
            // given
            int page = 0;
            int size = 10;
            OffsetPageResult<PostProvider> pageResult = new OffsetPageResult<>(1L, page, size, List.of(mockPostProvider));

            Mockito.doReturn(pageResult).when(queryRepository).fetchPostProviders(page, size);

            // when
            OffsetPageResult<PostProviderDto> result = postProviderService.getPostProviders(page, size);

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.getTotalCount()).isEqualTo(1L);
            Assertions.assertThat(result.getItems()).hasSize(1);
            Assertions.assertThat(result.getItems().get(0).id()).isEqualTo(mockPostProvider.id());
            Assertions.assertThat(result.getItems().get(0).name()).isEqualTo(mockPostProvider.name());
        }

        @Test
        void id를_입력하면_PostProviderDto를_조회할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockPostProvider)).when(queryRepository).fetchOneById(any());

            // when
            PostProviderDto result = postProviderService.getPostProviderDto(mockPostProvider.id());

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(mockPostProvider.id());
            Assertions.assertThat(result.name()).isEqualTo(mockPostProvider.name());
        }

        @Test
        void id를_입력하면_PostProvider_엔티티를_조회할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockPostProvider)).when(queryRepository).fetchOneById(any());

            // when
            PostProvider result = postProviderService.getPostProvider(mockPostProvider.id());

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(mockPostProvider.id());
        }

        @Test
        void id가_null이면_PostProvider_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postProviderService.getPostProvider(null))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }

        @Test
        void 존재하지_않는_id로_조회하면_PostProvider_조회에_실패한다() {
            // given
            UUID id = UUID.randomUUID();
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> postProviderService.getPostProvider(id))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("존재하지 않는 postProvider입니다.");
        }
    }

    @DisplayName("PostProvider 수정 테스트")
    @Nested
    class UpdatePostProviderTest {

        @Test
        void description과_baseUrl을_업데이트할_수_있다() {
            // given
            String newDescription = "updated_description";
            String newBaseUrl = "https://updated.com";
            PostProviderDto request = PostProviderDto.of(mockPostProvider.id(), newBaseUrl, newDescription, true);

            Mockito.doReturn(Optional.of(mockPostProvider)).when(queryRepository).fetchOneById(any());

            // when
            postProviderService.update(request);

            // then
            Assertions.assertThat(mockPostProvider.description()).isEqualTo(newDescription);
            Assertions.assertThat(mockPostProvider.baseUrl()).isEqualTo(newBaseUrl);
            Assertions.assertThat(mockPostProvider.isUsed()).isTrue();
        }

        @Test
        void isUsed_플래그만_업데이트할_수_있다() {
            // given
            PostProviderDto request = PostProviderDto.of(mockPostProvider.id(), null, null, false);

            Mockito.doReturn(Optional.of(mockPostProvider)).when(queryRepository).fetchOneById(any());

            String originalDescription = mockPostProvider.description();
            String originalBaseUrl = mockPostProvider.baseUrl();

            // when
            postProviderService.update(request);

            // then
            Assertions.assertThat(mockPostProvider.description()).isEqualTo(originalDescription);
            Assertions.assertThat(mockPostProvider.baseUrl()).isEqualTo(originalBaseUrl);
            Assertions.assertThat(mockPostProvider.isUsed()).isFalse();
        }

        @Test
        void id가_null이면_수정에_실패한다() {
            // given
            PostProviderDto request = PostProviderDto.of((UUID) null, "https://updated.com", "updated_description", true);

            // when & then
            Assertions.assertThatThrownBy(() -> postProviderService.update(request))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }
    }

    @DisplayName("PostProvider 삭제 테스트")
    @Nested
    class DeletePostProviderTest {

        @Test
        void id를_입력하면_PostProvider를_삭제할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockPostProvider)).when(queryRepository).fetchOneById(any());

            // when
            postProviderService.delete(mockPostProvider.id());

            // then
            Assertions.assertThat(mockPostProvider.isUsed()).isFalse();
            Assertions.assertThat(mockPostProvider.deletedAt()).isNotNull();
            Assertions.assertThat(mockPostProvider.isDelete()).isTrue();
        }

        @Test
        void id가_null이면_삭제에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postProviderService.delete(null))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }
    }
}
