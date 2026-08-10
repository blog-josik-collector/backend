package com.backend.userservice.userauthentication.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.exception.StateConflictException;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.UserAuthentication;
import com.backend.commondataaccess.persistence.user.enums.LoginProvider;
import com.backend.commondataaccess.persistence.user.enums.UserType;
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

@DisplayName("UserAuthenticationValidator 테스트")
class UserAuthenticationValidatorTest {

    @Nested
    @DisplayName("필수값 검증")
    class RequiredFields {

        @Test
        void user가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> UserAuthenticationValidator.validateUser(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("user");
        }

        @Test
        void id가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> UserAuthenticationValidator.validateId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id");
        }

        @Test
        void user_id가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> UserAuthenticationValidator.validateUserId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("user_id");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void identifier가_비어있으면_BadRequestException을_던진다(String identifier) {
            Assertions.assertThatThrownBy(() -> UserAuthenticationValidator.validateIdentifier(identifier))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("identifier");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void credential이_비어있으면_BadRequestException을_던진다(String credential) {
            Assertions.assertThatThrownBy(() -> UserAuthenticationValidator.validateCredential(credential))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("credential");
        }
    }

    @Nested
    @DisplayName("validateIsSameCredentialAndCredentialConfirm")
    class CredentialConfirm {

        @Test
        void credential과_confirm이_같으면_통과한다() {
            Assertions.assertThatCode(
                              () -> UserAuthenticationValidator.validateIsSameCredentialAndCredentialConfirm("pw", "pw"))
                      .doesNotThrowAnyException();
        }

        @Test
        void credential과_confirm이_다르면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(
                              () -> UserAuthenticationValidator.validateIsSameCredentialAndCredentialConfirm("pw", "other"))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("동일");
        }

        @Test
        void credential_confirm만_비어있어도_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(
                              () -> UserAuthenticationValidator.validateIsSameCredentialAndCredentialConfirm("pw", " "))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("credential_confirm");
        }
    }

    @Nested
    @DisplayName("verifyDuplicateIdentifier")
    class VerifyDuplicateIdentifier {

        @Test
        void 중복이_없으면_통과한다() {
            Assertions.assertThatCode(
                              () -> UserAuthenticationValidator.verifyDuplicateIdentifier("id", ignored -> false))
                      .doesNotThrowAnyException();
        }

        @Test
        void 이미_존재하면_StateConflictException을_던진다() {
            Assertions.assertThatThrownBy(
                              () -> UserAuthenticationValidator.verifyDuplicateIdentifier("id", ignored -> true))
                      .isInstanceOf(StateConflictException.class)
                      .hasMessageContaining("identifier");
        }
    }

    @Nested
    @DisplayName("getUserAuthenticationOrThrow")
    class GetOrThrow {

        @Test
        void id로_조회되면_반환한다() {
            UUID id = UUID.randomUUID();
            UserAuthentication auth = sampleAuth(id, "login");
            Function<UUID, Optional<UserAuthentication>> fetch = ignored -> Optional.of(auth);

            UserAuthentication found = UserAuthenticationValidator.getUserAuthenticationOrThrow(id, fetch);

            Assertions.assertThat(found).isSameAs(auth);
        }

        @Test
        void id로_없으면_NotFoundException을_던진다() {
            UUID id = UUID.randomUUID();

            Assertions.assertThatThrownBy(
                              () -> UserAuthenticationValidator.getUserAuthenticationOrThrow(
                                      id, ignored -> Optional.empty()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("id");
        }

        @Test
        void identifier로_조회되면_반환한다() {
            UserAuthentication auth = sampleAuth(UUID.randomUUID(), "login");
            Function<String, Optional<UserAuthentication>> fetch = ignored -> Optional.of(auth);

            UserAuthentication found =
                    UserAuthenticationValidator.getUserAuthenticationOrThrow("login", fetch);

            Assertions.assertThat(found).isSameAs(auth);
        }

        @Test
        void identifier로_없으면_NotFoundException을_던진다() {
            Assertions.assertThatThrownBy(
                              () -> UserAuthenticationValidator.getUserAuthenticationOrThrow(
                                      "missing", ignored -> Optional.empty()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("identifier");
        }

        private UserAuthentication sampleAuth(UUID id, String identifier) {
            User user = User.builder()
                            .id(UUID.randomUUID())
                            .userType(UserType.USER)
                            .nickname("nick")
                            .build();
            return UserAuthentication.builder()
                                     .id(id)
                                     .user(user)
                                     .loginProvider(LoginProvider.LOCAL)
                                     .identifier(identifier)
                                     .credential("pw")
                                     .build();
        }
    }
}
