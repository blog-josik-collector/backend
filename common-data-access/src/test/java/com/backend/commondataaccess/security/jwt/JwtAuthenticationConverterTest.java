package com.backend.commondataaccess.security.jwt;

import com.backend.commondataaccess.exception.UnauthorizedException;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.UserAuthentication;
import com.backend.commondataaccess.persistence.user.enums.LoginProvider;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.commondataaccess.security.JwtAuthenticationToken;
import com.backend.commondataaccess.security.JwtPrincipal;
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

@DisplayName("JwtAuthenticationConverter 테스트")
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationConverterTest {

    @InjectMocks
    private JwtAuthenticationConverter converter;

    @Mock
    private JwtService jwtService;

    @Test
    void 토큰을_검증해_ROLE_접두사가_붙은_인증_완료_토큰으로_변환한다() {
        UUID authenticationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                        .id(userId)
                        .userType(UserType.ADMIN)
                        .nickname("test_nickname")
                        .build();
        UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                  .id(authenticationId)
                                                                  .user(user)
                                                                  .loginProvider(LoginProvider.LOCAL)
                                                                  .identifier("login")
                                                                  .credential("pw")
                                                                  .build();
        String[] roles = {UserType.USER.name(), UserType.ADMIN.name()};

        Mockito.when(jwtService.verify("access.jwt.token")).thenReturn(Claims.of(userAuthentication, roles));

        Authentication authentication = converter.convertToAuthentication("access.jwt.token");

        Assertions.assertThat(authentication).isInstanceOf(JwtAuthenticationToken.class);
        Assertions.assertThat(authentication.isAuthenticated()).isTrue();
        Assertions.assertThat(authentication.getAuthorities())
                  .extracting(GrantedAuthority::getAuthority)
                  .containsExactly("ROLE_USER", "ROLE_ADMIN");

        JwtPrincipal principal = (JwtPrincipal) authentication.getPrincipal();
        Assertions.assertThat(principal.getId()).isEqualTo(authenticationId);
        Assertions.assertThat(principal.getUserId()).isEqualTo(userId);
        Assertions.assertThat(principal.getNickname()).isEqualTo("test_nickname");
        Assertions.assertThat(principal.getRoles()).containsExactly(roles);
    }

    @Test
    void 토큰_검증에_실패하면_UnauthorizedException을_그대로_던진다() {
        Mockito.when(jwtService.verify("invalid")).thenThrow(new UnauthorizedException());

        Assertions.assertThatThrownBy(() -> converter.convertToAuthentication("invalid"))
                  .isInstanceOf(UnauthorizedException.class);
    }
}
