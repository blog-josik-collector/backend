package com.backend.integratedworker.collectsourcepost.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("CollectSourcePostValidator 테스트")
class CollectSourcePostValidatorTest {

    @Nested
    @DisplayName("필수값 검증")
    class RequiredFields {

        @Test
        void id가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> CollectSourcePostValidator.validateId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void url이_비어있으면_BadRequestException을_던진다(String url) {
            Assertions.assertThatThrownBy(() -> CollectSourcePostValidator.validateUrl(url))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("url");
        }

        @Test
        void url이_있으면_통과한다() {
            Assertions.assertThatCode(() -> CollectSourcePostValidator.validateUrl("https://test.com/blog/1/post/1"))
                      .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getCollectSourcePostOrThrow")
    class GetCollectSourcePostOrThrow {

        @Test
        void 존재하면_CollectSourcePost를_반환한다() {
            UUID id = UUID.randomUUID();
            CollectSourcePost collectSourcePost = CollectSourcePost.builder()
                                                                   .id(id)
                                                                   .title("test_title")
                                                                   .url("https://test.com/blog/1/post/1")
                                                                   .build();
            Function<UUID, Optional<CollectSourcePost>> fetch = ignored -> Optional.of(collectSourcePost);

            CollectSourcePost found = CollectSourcePostValidator.getCollectSourcePostOrThrow(id, fetch);

            Assertions.assertThat(found).isSameAs(collectSourcePost);
        }

        @Test
        void 없으면_NotFoundException을_던진다() {
            UUID id = UUID.randomUUID();

            Assertions.assertThatThrownBy(
                              () -> CollectSourcePostValidator.getCollectSourcePostOrThrow(id, ignored -> Optional.empty()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 id");
        }
    }
}
