package com.backend.commondataaccess.security;

import com.backend.commondataaccess.exception.UnauthorizedException;
import com.backend.commondataaccess.security.jwt.JwtAuthenticationConverter;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("JwtAuthenticationFilter 테스트")
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @Mock
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private final MockFilterChain chain = new MockFilterChain();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_Bearer_토큰이면_SecurityContext에_인증정보를_넣는다() throws Exception {
        Authentication authentication = authentication();
        request.addHeader("Authorization", "Bearer access.jwt.token");
        Mockito.when(jwtAuthenticationConverter.convertToAuthentication("access.jwt.token")).thenReturn(authentication);

        filter.doFilter(request, response, chain);

        Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        Assertions.assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void Bearer_토큰은_URL_디코딩한_뒤_변환한다() throws Exception {
        request.addHeader("Authorization", "Bearer a%2Bb");
        Mockito.when(jwtAuthenticationConverter.convertToAuthentication("a+b")).thenReturn(authentication());

        filter.doFilter(request, response, chain);

        Mockito.verify(jwtAuthenticationConverter).convertToAuthentication("a+b");
    }

    @Test
    void Authorization_헤더가_없으면_인증정보_없이_chain을_진행한다() throws Exception {
        filter.doFilter(request, response, chain);

        Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        Assertions.assertThat(chain.getRequest()).isSameAs(request);
        Mockito.verifyNoInteractions(jwtAuthenticationConverter);
    }

    @Test
    void Bearer_형식이_아니면_토큰을_변환하지_않는다() throws Exception {
        request.addHeader("Authorization", "Basic dXNlcjpwdw==");

        filter.doFilter(request, response, chain);

        Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        Mockito.verifyNoInteractions(jwtAuthenticationConverter);
    }

    @Test
    void 토큰_검증에_실패하면_SecurityContext를_비우고_chain을_진행한다() throws Exception {
        request.addHeader("Authorization", "Bearer invalid");
        Mockito.when(jwtAuthenticationConverter.convertToAuthentication("invalid")).thenThrow(new UnauthorizedException());

        filter.doFilter(request, response, chain);

        Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        Assertions.assertThat(chain.getRequest()).isSameAs(request);
    }

    @Test
    void 토큰_디코딩에_실패하면_SecurityContext를_비우고_chain을_진행한다() throws Exception {
        request.addHeader("Authorization", "Bearer %zz");

        filter.doFilter(request, response, chain);

        Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        Assertions.assertThat(chain.getRequest()).isSameAs(request);
        Mockito.verifyNoInteractions(jwtAuthenticationConverter);
    }

    @Test
    void 이미_인증정보가_있으면_토큰을_다시_변환하지_않는다() throws Exception {
        Authentication existing = authentication();
        SecurityContextHolder.getContext().setAuthentication(existing);
        request.addHeader("Authorization", "Bearer access.jwt.token");

        filter.doFilter(request, response, chain);

        Assertions.assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(existing);
        Mockito.verify(jwtAuthenticationConverter, Mockito.never()).convertToAuthentication(ArgumentMatchers.anyString());
    }

    private static Authentication authentication() {
        JwtPrincipal principal = JwtPrincipal.builder()
                                             .id(UUID.randomUUID())
                                             .userId(UUID.randomUUID())
                                             .nickname("test_nickname")
                                             .roles(new String[]{"USER"})
                                             .build();
        return JwtAuthenticationToken.of(principal, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
