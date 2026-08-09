package com.backend.commondataaccess.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backend.commondataaccess.exception.UnauthorizedException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("CurrentUserArgumentResolver 테스트")
class CurrentUserArgumentResolverTest {

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 인증된_요청이면_principal을_주입한다() throws Exception {
        JwtPrincipal principal = principal();
        SecurityContextHolder.getContext()
                             .setAuthentication(JwtAuthenticationToken.of(principal, List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(resolver.resolveArgument(parameterOf("required"), null, null, null)).isSameAs(principal);
    }

    @Test
    void required가_true인데_인증정보가_없으면_401로_이어진다() throws Exception {
        assertThatThrownBy(() -> resolver.resolveArgument(parameterOf("required"), null, null, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void 익명_인증은_인증된_사용자로_보지_않는다() throws Exception {
        SecurityContextHolder.getContext()
                             .setAuthentication(new AnonymousAuthenticationToken("key", "anonymousUser",
                                                                                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(() -> resolver.resolveArgument(parameterOf("required"), null, null, null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void required가_false면_미인증_요청에_null을_반환한다() throws Exception {
        assertThat(resolver.resolveArgument(parameterOf("optional"), null, null, null)).isNull();
    }

    @Test
    void CurrentUser가_붙지_않은_파라미터는_처리하지_않는다() throws Exception {
        assertThat(resolver.supportsParameter(parameterOf("required"))).isTrue();
        assertThat(resolver.supportsParameter(parameterOf("optional"))).isTrue();
        assertThat(resolver.supportsParameter(parameterOf("notAnnotated"))).isFalse();
    }

    private static JwtPrincipal principal() {
        return JwtPrincipal.builder()
                           .id(UUID.randomUUID())
                           .userId(UUID.randomUUID())
                           .nickname("test_nickname")
                           .roles(new String[]{"USER"})
                           .build();
    }

    private static MethodParameter parameterOf(String methodName) throws NoSuchMethodException {
        return new MethodParameter(SampleController.class.getDeclaredMethod(methodName, JwtPrincipal.class), 0);
    }

    @SuppressWarnings("unused")
    static class SampleController {

        void required(@CurrentUser JwtPrincipal principal) {
        }

        void optional(@CurrentUser(required = false) JwtPrincipal principal) {
        }

        void notAnnotated(JwtPrincipal principal) {
        }
    }
}
