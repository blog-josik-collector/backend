package com.backend.commondataaccess.security;

import com.backend.commondataaccess.security.jwt.JwtAuthenticationConverter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 역할: 모든 요청에서 Authorization: Bearer ... 토큰을 확인하고, 유효하면 SecurityContext에 인증 정보를 주입하는 공통 필터. <p> 호출 위치: JwtAuthenticationFilter가 인증이 필요한 요청에서 호출. <p> 책임 <p> - 이미 인증된 요청이면 재처리하지 않음(SecurityContext 체크)
 * <p> - 헤더에서 Bearer 토큰 추출(resolveToken) <p> - 토큰이 유효하면 Converter를 통해 Authentication 생성 후 SecurityContext에 set <p> 비책임(두면 헷갈리는 영역) <p> - 토큰 생성/리프레시 정책 <p> - 클레임/권한 조립의 상세 로직(Converter 책임) <p>
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. 이미 인증된 요청인지 확인 (불필요한 파싱 방지)
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = resolveToken(request);

            if (StringUtils.hasText(token)) {
                // 2. 토큰 검증 및 Claims 추출
                Authentication authentication = jwtAuthenticationConverter.convertToAuthentication(token);
                // 3. 인증 객체 생성 및 Context 설정
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            try {
                // Bearer 이후의 실제 토큰값만 반환
                return URLDecoder.decode(bearerToken.substring(7), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("Token decoding failed", e);
            }
        }
        return null;
    }
}
