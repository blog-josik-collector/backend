package com.backend.interactionservice.post.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PostValidator 테스트")
class PostValidatorTest {

    @Nested
    @DisplayName("필수값 검증")
    class RequiredFields {

        @Test
        void id가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostValidator.validateId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }

        @Test
        void id가_있으면_통과한다() {
            Assertions.assertThatCode(() -> PostValidator.validateId(UUID.randomUUID()))
                      .doesNotThrowAnyException();
        }

        @Test
        void userId가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> PostValidator.validateUserId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }

        @Test
        void userId가_있으면_통과한다() {
            Assertions.assertThatCode(() -> PostValidator.validateUserId(UUID.randomUUID()))
                      .doesNotThrowAnyException();
        }
    }
}
