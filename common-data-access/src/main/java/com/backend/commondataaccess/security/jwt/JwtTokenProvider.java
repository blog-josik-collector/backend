package com.backend.commondataaccess.security.jwt;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.security.JwtAuthentication;
import com.backend.commondataaccess.security.JwtAuthenticationToken;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * 토큰 생성 및 검증 로직을 담습니다.
 */
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final Jwt jwt;

    public Authentication getAuthentication(String token) {
        Jwt.Claims claims = jwt.verify(token);

        List<GrantedAuthority> authorities = Arrays.stream(claims.getRoles())
                                                   .map(SimpleGrantedAuthority::new)
                                                   .collect(Collectors.toList());

        // 앞서 정의한 JwtAuthentication (Principal) 생성
        JwtAuthentication principal = JwtAuthentication.from(claims);

        return JwtAuthenticationToken.of(principal, authorities);
    }

    public boolean validateToken(String token) {
        try {
            jwt.verify(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String createToken(User user, String[] roles) {
        return jwt.createToken(user, roles);
    }
}
