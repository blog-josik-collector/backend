package com.backend.interactionservice.postbookmark.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.post.PostBookmark;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PostBookmarkValidator 테스트")
class PostBookmarkValidatorTest {

    @Nested
    @DisplayName("필수값 검증")
    class RequiredFields {

        @Test
        void userId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostBookmarkValidator.validateUserId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }

        @Test
        void postId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostBookmarkValidator.validatePostId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("postId는 필수 입력값입니다.");
        }
    }

    @Nested
    @DisplayName("getPostBookmarkOrThrow")
    class GetPostBookmarkOrThrow {

        @Test
        void 존재하면_PostBookmark를_반환한다() {
            PostBookmark postBookmark = PostBookmark.builder()
                                                    .id(UUID.randomUUID())
                                                    .isEnable(true)
                                                    .build();

            PostBookmark found = PostBookmarkValidator.getPostBookmarkOrThrow(UUID.randomUUID(),
                                                                              UUID.randomUUID(),
                                                                              (userId, postId) -> Optional.of(postBookmark));

            Assertions.assertThat(found).isSameAs(postBookmark);
        }

        @Test
        void 없으면_NotFoundException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostBookmarkValidator.getPostBookmarkOrThrow(UUID.randomUUID(),
                                                                                             UUID.randomUUID(),
                                                                                             (userId, postId) -> Optional.empty()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 postBookmark입니다.");
        }

        @Test
        void postId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostBookmarkValidator.getPostBookmarkOrThrow(UUID.randomUUID(),
                                                                                             null,
                                                                                             (userId, postId) -> Optional.empty()))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("postId는 필수 입력값입니다.");
        }
    }
}
