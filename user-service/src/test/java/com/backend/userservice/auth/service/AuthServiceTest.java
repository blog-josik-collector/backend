package com.backend.userservice.auth.service;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.UserAuthentication;
import com.backend.commondataaccess.persistence.user.enums.LoginProvider;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.commondataaccess.security.JwtAuthenticationToken;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.userservice.auth.oauth.google.GoogleOAuthUserClaims;
import com.backend.userservice.auth.service.dto.AuthDto;
import com.backend.userservice.auth.service.dto.AuthDto.GoogleRequest;
import com.backend.userservice.auth.service.dto.AuthDto.Response;
import com.backend.userservice.userauthentication.service.UserAuthenticationService;
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
import org.springframework.security.authentication.AuthenticationManager;

@DisplayName("AuthService 테스트")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Spy
    @InjectMocks
    private AuthService authService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserAuthenticationService userAuthenticationService;

    private User mockUser;

    @BeforeEach
    void init() {
        mockUser = User.builder()
                       .id(UUID.randomUUID())
                       .userType(UserType.USER)
                       .nickname("test_nickname")
                       .build();
    }

    @DisplayName("loginWithPassword 테스트")
    @Nested
    class LoginWithPasswordTest {

        @Test
        void login_id와_password를_입력하면_로그인에_성공한다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";
            String accessToken = "test_access_token";

            AuthDto.PasswordRequest request = AuthDto.PasswordRequest.of(loginId, password);

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.LOCAL)
                                                                      .identifier(loginId)
                                                                      .credential(password)
                                                                      .build();

            JwtPrincipal principal = JwtPrincipal.builder()
                                                 .id(UUID.randomUUID())
                                                 .userId(mockUser.id())
                                                 .nickname(mockUser.nickname())
                                                 .roles(new String[]{mockUser.userType().name()})
                                                 .build();

            JwtAuthenticationToken jwtAuthenticationToken = JwtAuthenticationToken.of(principal, createAuthorityList(mockUser.userType().name()));
            jwtAuthenticationToken.setDetails(accessToken);

            Mockito.doReturn(jwtAuthenticationToken).when(authenticationManager).authenticate(any());
            Mockito.doReturn(userAuthentication).when(userAuthenticationService).getUserAuthentication(any(String.class));

            Assertions.assertThat(userAuthentication.user().lastLoginAt()).isNull();

            // when
            Response response = authService.loginWithPassword(request);

            // then
            Assertions.assertThat(response).isNotNull();
            Assertions.assertThat(userAuthentication.user().lastLoginAt()).isNotNull();
            Assertions.assertThat(response.getAccessToken()).isEqualTo(accessToken);
        }
    }

    @DisplayName("loginWithGoogle 테스트")
    @Nested
    class LoginWithGoogleTest {

        @Test
        void google_subject를_입력하면_로그인에_성공한다() {
            // given
            String subject = "test_subject";
            String testEmail = "test_email";
            String testName = "test_name";
            String testPictureUrl = "test_picture_url";

            String accessToken = "test_access_token";

            GoogleOAuthUserClaims googleOAuthUserClaims = new GoogleOAuthUserClaims(subject,
                                                                                    testEmail,
                                                                                    true,
                                                                                    testName,
                                                                                    testPictureUrl);

            GoogleRequest request = GoogleRequest.from(googleOAuthUserClaims);

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.GOOGLE)
                                                                      .identifier(subject)
                                                                      .build();

            JwtPrincipal principal = JwtPrincipal.builder()
                                                 .id(UUID.randomUUID())
                                                 .userId(mockUser.id())
                                                 .nickname(mockUser.nickname())
                                                 .roles(new String[]{mockUser.userType().name()})
                                                 .build();

            JwtAuthenticationToken jwtAuthenticationToken = JwtAuthenticationToken.of(principal, createAuthorityList(mockUser.userType().name()));
            jwtAuthenticationToken.setDetails(accessToken);

            Mockito.doReturn(jwtAuthenticationToken).when(authenticationManager).authenticate(any());
            Mockito.doReturn(userAuthentication).when(userAuthenticationService).getUserAuthentication(any(String.class));

            Assertions.assertThat(userAuthentication.user().lastLoginAt()).isNull();

            // when
            Response response = authService.loginWithGoogle(request);

            // then
            Assertions.assertThat(response).isNotNull();
            Assertions.assertThat(userAuthentication.user().lastLoginAt()).isNotNull();
            Assertions.assertThat(response.getAccessToken()).isEqualTo(accessToken);
        }
    }
}