package com.backend.userservice.user.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.exception.StateConflictException;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.userservice.user.service.dto.UserDto;
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

@DisplayName("UserValidator 테스트")
class UserValidatorTest {

    @Nested
    @DisplayName("필수값 검증")
    class RequiredFields {

        @Test
        void id가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> UserValidator.validateId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id");
        }

        @Test
        void authentication_id가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> UserValidator.validateAuthenticationId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("authentication_id");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void login_id가_비어있으면_BadRequestException을_던진다(String loginId) {
            Assertions.assertThatThrownBy(() -> UserValidator.validateLoginId(loginId))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("login_id");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void subjectId가_비어있으면_BadRequestException을_던진다(String subjectId) {
            Assertions.assertThatThrownBy(() -> UserValidator.validateSubjectId(subjectId))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("subjectId");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void nickname이_비어있으면_BadRequestException을_던진다(String nickname) {
            Assertions.assertThatThrownBy(() -> UserValidator.validateNickname(nickname))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("nickname");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void password가_비어있으면_BadRequestException을_던진다(String password) {
            Assertions.assertThatThrownBy(() -> UserValidator.validatePassword(password))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("password");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void password_confirm이_비어있으면_BadRequestException을_던진다(String passwordConfirm) {
            Assertions.assertThatThrownBy(() -> UserValidator.validatePasswordConfirm(passwordConfirm))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("password_confirm");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void new_password가_비어있으면_BadRequestException을_던진다(String newPassword) {
            Assertions.assertThatThrownBy(() -> UserValidator.validateNewPassword(newPassword))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("new_password");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void access_token이_비어있으면_BadRequestException을_던진다(String accessToken) {
            Assertions.assertThatThrownBy(() -> UserValidator.validateAccessToken(accessToken))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("access_token");
        }
    }

    @Nested
    @DisplayName("verifyDuplicateNickname")
    class VerifyDuplicateNickname {

        @Test
        void 중복이_없으면_통과한다() {
            Assertions.assertThatCode(
                              () -> UserValidator.verifyDuplicateNickname("nick", nickname -> false))
                      .doesNotThrowAnyException();
        }

        @Test
        void 이미_존재하면_StateConflictException을_던진다() {
            Assertions.assertThatThrownBy(
                              () -> UserValidator.verifyDuplicateNickname("nick", nickname -> true))
                      .isInstanceOf(StateConflictException.class)
                      .hasMessageContaining("nickname");
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
            Function<UUID, Optional<User>> fetch = ignored -> Optional.of(user);

            User found = UserValidator.getUserOrThrow(id, fetch);

            Assertions.assertThat(found).isSameAs(user);
        }

        @Test
        void 없으면_NotFoundException을_던진다() {
            UUID id = UUID.randomUUID();

            Assertions.assertThatThrownBy(
                              () -> UserValidator.getUserOrThrow(id, ignored -> Optional.empty()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("user");
        }
    }

    @Nested
    @DisplayName("UnaryOperator")
    class UnaryOperators {

        @Test
        void validateId_loginId_nickname_password_체인이_유효_DTO를_통과시킨다() {
            UUID id = UUID.randomUUID();
            UserDto dto = UserDto.builder()
                                 .userId(id)
                                 .loginId("login")
                                 .nickname("nick")
                                 .password("pw")
                                 .passwordConfirm("pw")
                                 .build();

            UserDto result = UserValidator.validateId()
                                          .andThen(UserValidator.validateLoginId())
                                          .andThen(UserValidator.validateNickname())
                                          .andThen(UserValidator.validatePasswordAndPasswordConfirm())
                                          .apply(dto);

            Assertions.assertThat(result).isSameAs(dto);
        }
    }
}
