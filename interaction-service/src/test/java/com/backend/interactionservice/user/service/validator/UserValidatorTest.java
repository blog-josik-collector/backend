package com.backend.interactionservice.user.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UserValidator 테스트")
class UserValidatorTest {

    @Nested
    @DisplayName("필수값 검증")
    class RequiredFields {

        @Test
        void id가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> UserValidator.validateId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }

        @Test
        void id가_있으면_통과한다() {
            Assertions.assertThatCode(() -> UserValidator.validateId(UUID.randomUUID()))
                      .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getUserOrThrow")
    class GetUserOrThrow {

        @Test
        void 존재하면_User를_반환한다() {
            UUID id = UUID.randomUUID();
            User user = User.builder()
                            .id(id)
                            .userType(UserType.USER)
                            .nickname("nick")
                            .build();

            User found = UserValidator.getUserOrThrow(id, ignored -> Optional.of(user));

            Assertions.assertThat(found).isSameAs(user);
        }

        @Test
        void 없으면_NotFoundException을_던진다() {
            UUID id = UUID.randomUUID();

            Assertions.assertThatThrownBy(() -> UserValidator.getUserOrThrow(id, ignored -> Optional.empty()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 user입니다.");
        }

        @Test
        void id가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> UserValidator.getUserOrThrow(null, ignored -> Optional.empty()))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }
    }
}
