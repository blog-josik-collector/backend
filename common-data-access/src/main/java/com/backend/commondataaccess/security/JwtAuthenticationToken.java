package com.backend.commondataaccess.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    /**
     * 인증 주체를 나타낸다. 보통 UserDetails나 사용자 ID(UUID) 의미
     */
    private final Object principal;

    private Object credentials;     // JWT 토큰 자체

    public static JwtAuthenticationToken from(String principal, String credentials) {
        return new JwtAuthenticationToken(principal, credentials);
    }

    public static JwtAuthenticationToken of(Object principal, Collection<? extends GrantedAuthority> authorities) {
        return new JwtAuthenticationToken(principal, authorities);
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
