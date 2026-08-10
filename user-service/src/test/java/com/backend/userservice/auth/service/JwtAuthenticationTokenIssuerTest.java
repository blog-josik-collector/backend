package com.backend.userservice.auth.service;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.UserAuthentication;
import com.backend.commondataaccess.persistence.user.enums.LoginProvider;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.commondataaccess.security.JwtAuthenticationToken;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.commondataaccess.security.jwt.JwtService;
import com.backend.commondataaccess.security.jwt.JwtService.Claims;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

@DisplayName("JwtAuthenticationTokenIssuer 테스트")
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationTokenIssuerTest {

    @InjectMocks
    private JwtAuthenticationTokenIssuer issuer;

    @Mock
    private JwtService jwtService;

    @Test
    void UserAuthentication으로_인증_완료_토큰과_accessToken_details를_만든다() {
        UUID authenticationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                        .id(userId)
                        .userType(UserType.USER)
                        .nickname("test_nickname")
                        .build();
        UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                  .id(authenticationId)
                                                                  .user(user)
                                                                  .loginProvider(LoginProvider.LOCAL)
                                                                  .identifier("login")
                                                                  .credential("pw")
                                                                  .build();

        String accessToken = "access.jwt.token";
        Claims claims = Claims.of(userAuthentication, new String[]{UserType.USER.name()});

        Mockito.when(jwtService.createToken(userAuthentication, new String[]{UserType.USER.name()}))
               .thenReturn(accessToken);
        Mockito.when(jwtService.verify(accessToken)).thenReturn(claims);

        Authentication authentication = issuer.issue(userAuthentication);

        Assertions.assertThat(authentication).isInstanceOf(JwtAuthenticationToken.class);
        Assertions.assertThat(authentication.isAuthenticated()).isTrue();
        Assertions.assertThat(authentication.getDetails()).isEqualTo(accessToken);
        Assertions.assertThat(authentication.getAuthorities())
                  .extracting(GrantedAuthority::getAuthority)
                  .containsExactly(UserType.USER.name());

        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Assertions.assertThat(principal.getId()).isEqualTo(authenticationId);
        Assertions.assertThat(principal.getUserId()).isEqualTo(userId);
        Assertions.assertThat(principal.getNickname()).isEqualTo("test_nickname");
    }

    @Test
    void ADMIN_유저면_role에_ADMIN을_넣는다() {
        User user = User.builder()
                        .id(UUID.randomUUID())
                        .userType(UserType.ADMIN)
                        .nickname("admin")
                        .build();
        UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                  .id(UUID.randomUUID())
                                                                  .user(user)
                                                                  .loginProvider(LoginProvider.LOCAL)
                                                                  .identifier("admin")
                                                                  .credential("pw")
                                                                  .build();

        String accessToken = "admin.token";
        Claims claims = Claims.of(userAuthentication, new String[]{UserType.ADMIN.name()});

        Mockito.when(jwtService.createToken(userAuthentication, new String[]{UserType.ADMIN.name()}))
               .thenReturn(accessToken);
        Mockito.when(jwtService.verify(accessToken)).thenReturn(claims);

        Authentication authentication = issuer.issue(userAuthentication);

        Assertions.assertThat(authentication.getAuthorities())
                  .extracting(GrantedAuthority::getAuthority)
                  .containsExactly(UserType.ADMIN.name());
    }
}
