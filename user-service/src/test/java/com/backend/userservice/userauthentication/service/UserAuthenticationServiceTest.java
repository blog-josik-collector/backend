package com.backend.userservice.userauthentication.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.UserAuthentication;
import com.backend.commondataaccess.persistence.user.enums.LoginProvider;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.userservice.userauthentication.repository.UserAuthenticationQueryRepository;
import com.backend.userservice.userauthentication.repository.UserAuthenticationRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;

@DisplayName("UserAuthenticationService 테스트")
@ExtendWith(MockitoExtension.class)
class UserAuthenticationServiceTest {

    @Spy
    @InjectMocks
    private UserAuthenticationService userAuthenticationService;

    @Mock
    private UserAuthenticationRepository userAuthenticationRepository;

    @Mock
    private UserAuthenticationQueryRepository queryRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private User mockUser;

    @BeforeEach
    void init() {
        mockUser = User.builder()
                       .id(UUID.randomUUID())
                       .userType(UserType.USER)
                       .nickname("test_nickname")
                       .build();
    }


    @DisplayName("UserAuthentication 생성 테스트")
    @Nested
    class CreateUserAuthenticationTest {

        @Test
        void user와_loginId와_password와_passwordConfirm을_입력하면_UserAuthentication_생성한다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";
            String passwordConfirm = "test_password";
            String encodedPassword = "test_encoded_password";

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .id(UUID.randomUUID())
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.LOCAL)
                                                                      .identifier(loginId)
                                                                      .credential(password)
                                                                      .build();

            Mockito.doReturn(userAuthentication).when(userAuthenticationRepository).save(any());
            Mockito.doReturn(encodedPassword).when(passwordEncoder).encode(any());

            // when
            UserAuthentication savedUserAuthentication = userAuthenticationService.create(mockUser,
                                                                                          loginId,
                                                                                          password,
                                                                                          passwordConfirm);

            // then
            Assertions.assertThat(savedUserAuthentication).isNotNull();
            Assertions.assertThat(savedUserAuthentication.identifier()).isEqualTo(loginId);
            Assertions.assertThat(savedUserAuthentication.credential()).isEqualTo(password);
            Assertions.assertThat(savedUserAuthentication.user().id()).isEqualTo(mockUser.id());
        }

        @Test
        void user와_subject를_입력하면_UserAuthentication_생성한다() {
            // given
            String subject = "test_subject";

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .id(UUID.randomUUID())
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.GOOGLE)
                                                                      .identifier(subject)
                                                                      .build();

            Mockito.doReturn(userAuthentication).when(userAuthenticationRepository).save(any());

            // when
            UserAuthentication savedUserAuthentication = userAuthenticationService.create(mockUser, subject);

            // then
            Assertions.assertThat(savedUserAuthentication).isNotNull();
            Assertions.assertThat(savedUserAuthentication.identifier()).isEqualTo(subject);
            Assertions.assertThat(savedUserAuthentication.credential()).isNull();
            Assertions.assertThat(savedUserAuthentication.user().id()).isEqualTo(mockUser.id());
        }

        @Test
        void password와_passwordConfirm이_다르면_UserAuthentication_생성에_실패한다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";
            String passwordConfirm = "not_equal_password_confirm";

            // when & then
            Assertions.assertThatThrownBy(() -> userAuthenticationService.create(mockUser, loginId, password, passwordConfirm))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessage("credential와 credential_confirm은 값은 동일해야합니다.");
        }

        @Test
        void loginId_또는_subject_가_중복되면_UserAuthentication_생성에_실패한다() {
            // given
            String duplicatedLoginId = "duplicated_login_id";
            String password = "test_password";
            String passwordConfirm = "test_password";
            String duplicatedSubject = "duplicated_subject";

            Mockito.doReturn(Boolean.TRUE).when(userAuthenticationRepository).existsByIdentifier(any());

            // when & then
            Assertions.assertThatThrownBy(() -> userAuthenticationService.create(mockUser, duplicatedLoginId, password, passwordConfirm))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("이미 존재하는 identifier입니다.");

            Assertions.assertThatThrownBy(() -> userAuthenticationService.create(mockUser, duplicatedSubject))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("이미 존재하는 identifier입니다.");
        }
    }

    @DisplayName("UserAuthentication 조회 테스트")
    @Nested
    class ReadUserAuthenticationTest {

        @Test
        void id를_입력하면_UserAuthentication_를_조회할_수_있다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .id(UUID.randomUUID())
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.LOCAL)
                                                                      .identifier(loginId)
                                                                      .credential(password)
                                                                      .build();

            Mockito.doReturn(Optional.of(userAuthentication)).when(queryRepository).findOneById(any());

            // when
            UserAuthentication result = userAuthenticationService.getUserAuthentication(userAuthentication.id());

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(userAuthentication.id());
            Assertions.assertThat(result.identifier()).isEqualTo(userAuthentication.identifier());
            Assertions.assertThat(result.credential()).isEqualTo(userAuthentication.credential());
            Assertions.assertThat(result.user().id()).isEqualTo(userAuthentication.user().id());
        }

        @Test
        void identifier를_입력하면_UserAuthentication_를_조회할_수_있다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .id(UUID.randomUUID())
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.LOCAL)
                                                                      .identifier(loginId)
                                                                      .credential(password)
                                                                      .build();

            Mockito.doReturn(Optional.of(userAuthentication)).when(queryRepository).findOneByIdentifier(any());

            // when
            UserAuthentication result = userAuthenticationService.getUserAuthentication(userAuthentication.identifier());

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.id()).isEqualTo(userAuthentication.id());
            Assertions.assertThat(result.identifier()).isEqualTo(userAuthentication.identifier());
            Assertions.assertThat(result.credential()).isEqualTo(userAuthentication.credential());
            Assertions.assertThat(result.user().id()).isEqualTo(userAuthentication.user().id());
        }

        @Test
        void user_id를_입력하면_user_id와_매핑되는_UserAuthentication_목록을_조회할_수_있다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .id(UUID.randomUUID())
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.LOCAL)
                                                                      .identifier(loginId)
                                                                      .credential(password)
                                                                      .build();

            Mockito.doReturn(List.of(userAuthentication)).when(queryRepository).findAllByUserId(any());

            // when
            List<UserAuthentication> results = userAuthenticationService.getUserAuthenticationsByUser(mockUser.id());

            // then
            Assertions.assertThat(results).isNotNull();
            Assertions.assertThat(results.get(0).id()).isEqualTo(userAuthentication.id());
            Assertions.assertThat(results.get(0).identifier()).isEqualTo(userAuthentication.identifier());
            Assertions.assertThat(results.get(0).credential()).isEqualTo(userAuthentication.credential());
            Assertions.assertThat(results.get(0).user().id()).isEqualTo(userAuthentication.user().id());
        }

        @Test
        void identifier를_입력하면_UserAuthentication_를_옵셔널로_조회할_수_있다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .id(UUID.randomUUID())
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.LOCAL)
                                                                      .identifier(loginId)
                                                                      .credential(password)
                                                                      .build();

            Mockito.doReturn(Optional.of(userAuthentication)).when(queryRepository).findOneByIdentifier(any());

            // when
            Optional<UserAuthentication> result = userAuthenticationService.findUserAuthentication(userAuthentication.identifier());

            // then
            Assertions.assertThat(result).isNotNull();

            if (result.isPresent()) {
                Assertions.assertThat(result.get().id()).isEqualTo(userAuthentication.id());
                Assertions.assertThat(result.get().identifier()).isEqualTo(userAuthentication.identifier());
                Assertions.assertThat(result.get().credential()).isEqualTo(userAuthentication.credential());
                Assertions.assertThat(result.get().user().id()).isEqualTo(userAuthentication.user().id());
            }
        }
    }

    @DisplayName("UserAuthentication 수정 테스트")
    @Nested
    class UpdateUserAuthenticationTest {

        @Test
        void password를_업데이트_할_수_있다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .id(UUID.randomUUID())
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.LOCAL)
                                                                      .identifier(loginId)
                                                                      .credential(password)
                                                                      .build();

            String newPassword = "test_new_password";
            String encodedNewPassword = "test_encoded_password";

            Mockito.doReturn(List.of(userAuthentication)).when(queryRepository).findAllByUserId(any());
            Mockito.doReturn(true).when(passwordEncoder).matches(any(), any());
            Mockito.doReturn(encodedNewPassword).when(passwordEncoder).encode(any());

            Assertions.assertThat(userAuthentication.credential()).isEqualTo(password);

            // when
            userAuthenticationService.updatePassword(mockUser.id(), password, newPassword);

            // then
            Assertions.assertThat(userAuthentication.credential()).isEqualTo(encodedNewPassword);
        }

        @Test
        void password_업데이트_시_기존_비밀번호가_틀리면_password를_업데이트_할_수_없다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .id(UUID.randomUUID())
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.LOCAL)
                                                                      .identifier(loginId)
                                                                      .credential(password)
                                                                      .build();

            String invalidPassword = "test_invalid_password";
            String newPassword = "test_new_password";

            Mockito.doReturn(List.of(userAuthentication)).when(queryRepository).findAllByUserId(any());
            Mockito.doReturn(false).when(passwordEncoder).matches(any(), any());

            // when & then
            Assertions.assertThatThrownBy(() -> userAuthenticationService.updatePassword(mockUser.id(), invalidPassword, newPassword))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("사용자의 password와 입력한 password가 다릅니다.");
        }

        @Test
        void password_업데이트_시_LOCAL_LOGIN_계정만_password를_업데이트_할_수_없다() {
            // given
            String subject = "test_subject";

            UserAuthentication oauthUserAuthentication = UserAuthentication.builder()
                                                                           .id(UUID.randomUUID())
                                                                           .user(mockUser)
                                                                           .loginProvider(LoginProvider.GOOGLE)
                                                                           .identifier(subject)
                                                                           .credential(null)
                                                                           .build();

            String password = "test_password";
            String newPassword = "test_new_password";

            Mockito.doReturn(List.of(oauthUserAuthentication)).when(queryRepository).findAllByUserId(any());

            // when & then
            Assertions.assertThatThrownBy(() -> userAuthenticationService.updatePassword(mockUser.id(), password, newPassword))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessage("LoginId/Password 입력 방식 계정만 비밀번호를 변경할 수 있습니다.");
        }

        @Test
        void 서로_다른_user_id를_가진_user_authentication_정보를_하나의_user_id로_merge_할_수_있다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";

            UserAuthentication localUserAuthentication = UserAuthentication.builder()
                                                                           .id(UUID.randomUUID())
                                                                           .user(mockUser)
                                                                           .loginProvider(LoginProvider.LOCAL)
                                                                           .identifier(loginId)
                                                                           .credential(password)
                                                                           .build();

            User mockUser2 = User.builder()
                                 .id(UUID.randomUUID())
                                 .userType(UserType.USER)
                                 .nickname("test_nickname")
                                 .build();

            String subject = "test_subject";

            UserAuthentication googleUserAuthentication = UserAuthentication.builder()
                                                                            .id(UUID.randomUUID())
                                                                            .user(mockUser2)
                                                                            .loginProvider(LoginProvider.GOOGLE)
                                                                            .identifier(subject)
                                                                            .build();

            Mockito.doReturn(Optional.of(localUserAuthentication)).when(queryRepository).findOneById(localUserAuthentication.id());
            Mockito.doReturn(List.of(googleUserAuthentication)).when(queryRepository).findAllByUserId(any());

            Assertions.assertThat(googleUserAuthentication.user().id()).isEqualTo(mockUser2.id());
            Assertions.assertThat(googleUserAuthentication.user().nickname()).isEqualTo(mockUser2.nickname());
            Assertions.assertThat(mockUser2.isDelete()).isEqualTo(false);

            // when
            userAuthenticationService.merge(localUserAuthentication.id(), mockUser2);

            // then
            Assertions.assertThat(googleUserAuthentication.user().id()).isEqualTo(mockUser.id());
            Assertions.assertThat(googleUserAuthentication.user().nickname()).isEqualTo(mockUser.nickname());
            Assertions.assertThat(mockUser2.isDelete()).isEqualTo(true);

            Assertions.assertThat(localUserAuthentication.user().id()).isEqualTo(mockUser.id());
            Assertions.assertThat(localUserAuthentication.user().nickname()).isEqualTo(mockUser.nickname());
        }

        @Test
        void 동일한_user_id_정보는_merge_할_수_없다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";

            UserAuthentication localUserAuthentication = UserAuthentication.builder()
                                                                           .id(UUID.randomUUID())
                                                                           .user(mockUser)
                                                                           .loginProvider(LoginProvider.LOCAL)
                                                                           .identifier(loginId)
                                                                           .credential(password)
                                                                           .build();

            Mockito.doReturn(Optional.of(localUserAuthentication)).when(queryRepository).findOneById(localUserAuthentication.id());

            // when & then
            Assertions.assertThatThrownBy(() -> userAuthenticationService.merge(localUserAuthentication.id(), mockUser))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("동일한 사용자끼리는 정보를 합칠 수 없습니다.");
        }
    }

    @DisplayName("UserAuthentication 삭제 테스트")
    @Nested
    class DeleteUserAuthenticationTest {

        @Test
        void 특정_사용자가_삭제되면_해당_사용자의_user_authentication도_전부_삭제된다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";

            UserAuthentication localUserAuthentication = UserAuthentication.builder()
                                                                           .id(UUID.randomUUID())
                                                                           .user(mockUser)
                                                                           .loginProvider(LoginProvider.LOCAL)
                                                                           .identifier(loginId)
                                                                           .credential(password)
                                                                           .build();

            String subject = "test_subject";

            UserAuthentication googleUserAuthentication = UserAuthentication.builder()
                                                                            .id(UUID.randomUUID())
                                                                            .user(mockUser)
                                                                            .loginProvider(LoginProvider.GOOGLE)
                                                                            .identifier(subject)
                                                                            .build();

            Mockito.doReturn(List.of(localUserAuthentication, googleUserAuthentication))
                   .when(queryRepository).findAllByUserId(any());

            // when
            userAuthenticationService.deleteAll(mockUser.id());

            // then
            Assertions.assertThat(localUserAuthentication.deletedAt()).isNotNull();
            Assertions.assertThat(localUserAuthentication.isDelete()).isEqualTo(true);
            Assertions.assertThat(googleUserAuthentication.deletedAt()).isNotNull();
            Assertions.assertThat(googleUserAuthentication.isDelete()).isEqualTo(true);
        }
    }
}