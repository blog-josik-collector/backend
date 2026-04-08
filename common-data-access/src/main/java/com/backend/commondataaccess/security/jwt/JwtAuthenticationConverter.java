package com.backend.commondataaccess.security.jwt;

import com.backend.commondataaccess.security.JwtPrincipal;
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
 * 역할: “JWT 문자열”을 받아서 Spring Security에서 쓰는 Authentication 객체로 변환하는 컴포넌트. <p> 호출 위치: JwtAuthenticationFilter가 인증이 필요한 요청에서 호출. <p> 책임 <p> - JwtTokenService.verify(token)로 Claims 획득 <p> - roles →
 * List.of(GrantedAuthority.class) 변환 <p> - JwtAuthentication(principal) 생성 <p> - JwtAuthenticationToken(인증 완료 Authentication) 생성 및 반환 <p> 비책임(두면 헷갈리는 영역) <p> - HTTP 요청에서 토큰을 추출하는 일(필터 책임) <p> -
 * SecurityContext에 넣는 일(필터 책임) <p> - 토큰 “발급”(JwtTokenService 책임) <p>
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationConverter {

    private final JwtService jwt;

    public Authentication convertToAuthentication(String token) {
        JwtService.Claims claims = jwt.verify(token);

        List<GrantedAuthority> authorities = Arrays.stream(claims.getRoles())
                                                   .map(SimpleGrantedAuthority::new)
                                                   .collect(Collectors.toList());

        // 앞서 정의한 JwtAuthentication (Principal) 생성
        JwtPrincipal principal = JwtPrincipal.from(claims);

        return JwtAuthenticationToken.of(principal, authorities);
    }
}
