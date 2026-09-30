package com.backend.interactionservice.postlike.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.post.PostLike;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PostLikeValidator 테스트")
class PostLikeValidatorTest {

    @Nested
    @DisplayName("필수값 검증")
    class RequiredFields {

        @Test
        void userId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostLikeValidator.validateUserId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }

        @Test
        void postId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostLikeValidator.validatePostId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("postId는 필수 입력값입니다.");
        }
    }

    @Nested
    @DisplayName("getPostLikeOrThrow")
    class GetPostLikeOrThrow {

        @Test
        void 존재하면_PostLike를_반환한다() {
            PostLike postLike = PostLike.builder()
                                        .id(UUID.randomUUID())
                                        .isEnable(true)
                                        .build();

            PostLike found = PostLikeValidator.getPostLikeOrThrow(UUID.randomUUID(),
                                                                  UUID.randomUUID(),
                                                                  (userId, postId) -> Optional.of(postLike));

            Assertions.assertThat(found).isSameAs(postLike);
        }

        @Test
        void 없으면_NotFoundException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostLikeValidator.getPostLikeOrThrow(UUID.randomUUID(),
                                                                                     UUID.randomUUID(),
                                                                                     (userId, postId) -> Optional.empty()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 postLike입니다.");
        }

        @Test
        void userId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostLikeValidator.getPostLikeOrThrow(null,
                                                                                     UUID.randomUUID(),
                                                                                     (userId, postId) -> Optional.empty()))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }
    }
}
