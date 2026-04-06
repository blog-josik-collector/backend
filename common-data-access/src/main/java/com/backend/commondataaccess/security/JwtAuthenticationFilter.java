package com.backend.commondataaccess.security;

import com.backend.commondataaccess.security.jwt.JwtTokenProvider;
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
 * (공통 필터) 모든 요청에서 토큰을 체크하는 필터입니다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // 1. 이미 인증된 요청인지 확인 (불필요한 파싱 방지)
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = resolveToken(request);

            if (StringUtils.hasText(token) && tokenProvider.validateToken(token)) {
                // 2. 토큰 검증 및 Claims 추출
                Authentication authentication = tokenProvider.getAuthentication(token);
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
