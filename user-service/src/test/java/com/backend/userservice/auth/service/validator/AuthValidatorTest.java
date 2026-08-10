package com.backend.userservice.auth.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.userservice.auth.service.dto.AuthDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AuthValidator 테스트")
class AuthValidatorTest {

    @Nested
    @DisplayName("validateUserId")
    class ValidateUserId {

        @Test
        void userId가_있으면_통과한다() {
            Assertions.assertThatCode(() -> AuthValidator.validateUserId("login_id"))
                      .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t"})
        void userId가_비어있으면_BadRequestException을_던진다(String userId) {
            Assertions.assertThatThrownBy(() -> AuthValidator.validateUserId(userId))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("user_id");
        }

        @Test
        void UnaryOperator는_유효한_PasswordRequest를_그대로_반환한다() {
            AuthDto.PasswordRequest request = AuthDto.PasswordRequest.of("id", "pw");

            AuthDto.PasswordRequest result = AuthValidator.validateUserId().apply(request);

            Assertions.assertThat(result).isSameAs(request);
        }
    }

    @Nested
    @DisplayName("validatePassword")
    class ValidatePassword {

        @Test
        void password가_있으면_통과한다() {
            Assertions.assertThatCode(() -> AuthValidator.validatePassword("secret"))
                      .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void password가_비어있으면_BadRequestException을_던진다(String password) {
            Assertions.assertThatThrownBy(() -> AuthValidator.validatePassword(password))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("password");
        }
    }

    @Nested
    @DisplayName("validateSubject")
    class ValidateSubject {

        @Test
        void subject가_있으면_통과한다() {
            Assertions.assertThatCode(() -> AuthValidator.validateSubject("google-sub"))
                      .doesNotThrowAnyException();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void subject가_비어있으면_BadRequestException을_던진다(String subject) {
            Assertions.assertThatThrownBy(() -> AuthValidator.validateSubject(subject))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("subject");
        }

        @Test
        void UnaryOperator는_유효한_GoogleRequest를_그대로_반환한다() {
            AuthDto.GoogleRequest request = AuthDto.GoogleRequest.builder()
                                                                 .subject("google-sub")
                                                                 .build();

            AuthDto.GoogleRequest result = AuthValidator.validateSubject().apply(request);

            Assertions.assertThat(result).isSameAs(request);
        }
    }
}
