package com.backend.commondataaccess.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * 역할: Spring Security가 이해하는 Authentication 구현체. <p> - 인증 전: from(userId, password)처럼 “credentials 기반” 미인증 토큰 <p> - 인증 후: of(principal, authorities)처럼 “권한 포함” 인증 완료 토큰 <p> 책임 <p> - 인증 전/후 상태
 * 표현(setAuthenticated 제약 포함) <p> - SecurityContext에 담길 표준 형태 제공 <p> 비책임(두면 헷갈리는 영역) <p> - 토큰 검증/파싱/발급 <p> - 권한 계산 로직 <p>
 */
public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    /**
     * 인증 주체를 나타낸다. 보통 UserDetails나 사용자 ID(UUID) 의미
     */
    private final Object principal;

    private Object credentials;     // JWT 토큰 자체

    public static JwtAuthenticationToken from(String principal) {
        return new JwtAuthenticationToken(principal);
    }

    public static JwtAuthenticationToken of(String principal, String credentials) {
        return new JwtAuthenticationToken(principal, credentials);
    }

    public static JwtAuthenticationToken of(Object principal, Collection<? extends GrantedAuthority> authorities) {
        return new JwtAuthenticationToken(principal, authorities);
    }

    private JwtAuthenticationToken(String principal) {
        super(null);
        super.setAuthenticated(false);

        this.principal = principal;
        this.credentials = null;
    }

    private JwtAuthenticationToken(String principal, String credentials) {
        super(null);
        super.setAuthenticated(false);

        this.principal = principal;
        this.credentials = credentials;
    }

    private JwtAuthenticationToken(Object principal, Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        super.setAuthenticated(true);

        this.principal = principal;
    }

    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        if (isAuthenticated) {
            throw new IllegalArgumentException("Cannot set this token to trusted - use constructor which takes a GrantedAuthority list instead");
        }
        super.setAuthenticated(false);
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return credentials;
    }
}
