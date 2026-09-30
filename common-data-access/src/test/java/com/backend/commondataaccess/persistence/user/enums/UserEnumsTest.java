package com.backend.commondataaccess.persistence.user.enums;

import com.backend.commondataaccess.exception.BadRequestException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("User enum / Converter 테스트")
class UserEnumsTest {

    @DisplayName("UserType")
    @Nested
    class UserTypeTest {

        private final UserTypeConverter converter = new UserTypeConverter();

        @ParameterizedTest
        @EnumSource(UserType.class)
        void code로_변환하고_다시_복원한다(UserType userType) {
            Integer code = converter.convertToDatabaseColumn(userType);

            Assertions.assertThat(code).isEqualTo(userType.getCode());
            Assertions.assertThat(converter.convertToEntityAttribute(code)).isEqualTo(userType);
        }

        @Test
        void 없는_code면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> UserType.from(0))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("UserType");
        }
    }

    @DisplayName("LoginType")
    @Nested
    class LoginTypeTest {

        private final LoginTypeConverter converter = new LoginTypeConverter();

        @ParameterizedTest
        @EnumSource(LoginType.class)
        void code로_변환하고_다시_복원한다(LoginType loginType) {
            Integer code = converter.convertToDatabaseColumn(loginType);

            Assertions.assertThat(code).isEqualTo(loginType.getCode());
            Assertions.assertThat(converter.convertToEntityAttribute(code)).isEqualTo(loginType);
        }

        @Test
        void 없는_code면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> LoginType.from(0))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("LoginType");
        }
    }

    @DisplayName("LoginProvider")
    @Nested
    class LoginProviderTest {

        private final LoginProviderConverter converter = new LoginProviderConverter();

        @ParameterizedTest
        @EnumSource(LoginProvider.class)
        void code로_변환하고_다시_복원한다(LoginProvider loginProvider) {
            Integer code = converter.convertToDatabaseColumn(loginProvider);

            Assertions.assertThat(code).isEqualTo(loginProvider.getCode());
            Assertions.assertThat(converter.convertToEntityAttribute(code)).isEqualTo(loginProvider);
        }

        @Test
        void null은_양방향_모두_null로_변환한다() {
            Assertions.assertThat(converter.convertToDatabaseColumn(null)).isNull();
            Assertions.assertThat(converter.convertToEntityAttribute(null)).isNull();
        }

        @Test
        void 없는_code면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> LoginProvider.from(0))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("LoginProvider");
        }
    }
}
