package com.backend.commondataaccess.security;

import com.backend.commondataaccess.exception.StateConflictException;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

@DisplayName("JwtAuthenticationToken 테스트")
class JwtAuthenticationTokenTest {

    @Test
    void from으로_만든_토큰은_credentials_없는_미인증_토큰이다() {
        JwtAuthenticationToken token = JwtAuthenticationToken.from("user");

        Assertions.assertThat(token.isAuthenticated()).isFalse();
        Assertions.assertThat(token.getPrincipal()).isEqualTo("user");
        Assertions.assertThat(token.getCredentials()).isNull();
        Assertions.assertThat(token.getAuthorities()).isEmpty();
    }

    @Test
    void principal과_credentials로_만든_토큰은_미인증_토큰이다() {
        JwtAuthenticationToken token = JwtAuthenticationToken.of("user", "password");

        Assertions.assertThat(token.isAuthenticated()).isFalse();
        Assertions.assertThat(token.getPrincipal()).isEqualTo("user");
        Assertions.assertThat(token.getCredentials()).isEqualTo("password");
    }

    @Test
    void 권한과_함께_만든_토큰은_인증_완료_토큰이다() {
        Object principal = new Object();
        JwtAuthenticationToken token = JwtAuthenticationToken.of(principal, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        Assertions.assertThat(token.isAuthenticated()).isTrue();
        Assertions.assertThat(token.getPrincipal()).isSameAs(principal);
        Assertions.assertThat(token.getCredentials()).isNull();
        Assertions.assertThat(token.getAuthorities())
                  .extracting(GrantedAuthority::getAuthority)
                  .containsExactly("ROLE_USER");
    }

    @Test
    void setAuthenticated_true는_StateConflictException을_던진다() {
        JwtAuthenticationToken token = JwtAuthenticationToken.from("user");

        Assertions.assertThatThrownBy(() -> token.setAuthenticated(true))
                  .isInstanceOf(StateConflictException.class);
        Assertions.assertThat(token.isAuthenticated()).isFalse();
    }

    @Test
    void setAuthenticated_false는_인증_완료_토큰을_미인증으로_되돌린다() {
        JwtAuthenticationToken token = JwtAuthenticationToken.of(new Object(), List.of(new SimpleGrantedAuthority("ROLE_USER")));

        token.setAuthenticated(false);

        Assertions.assertThat(token.isAuthenticated()).isFalse();
    }
}
