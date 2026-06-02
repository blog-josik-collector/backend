package com.backend.commondataaccess.security;

import com.backend.commondataaccess.exception.UnauthorizedException;
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
 * 역할: 모든 요청에서 Authorization: Bearer ... 토큰을 확인하고, 유효하면 SecurityContext에 인증 정보를 주입하는 공통 필터. <p>
 * 유효하지 않은 Bearer 토큰은 {@link UnauthorizedException} 으로 처리한 뒤 SecurityContext 를 비우고 chain 을 계속한다.
 * protected URL 이면 {@link org.springframework.security.web.AuthenticationEntryPoint} 가 동일한 ErrorResponse JSON(401)을 반환한다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String token = resolveToken(request);
                if (StringUtils.hasText(token)) {
                    Authentication authentication = jwtAuthenticationConverter.convertToAuthentication(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (UnauthorizedException e) {
                SecurityContextHolder.clearContext();
                log.debug("[Auth][BE40101] invalid JWT", e);
            }
        }

        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            try {
                return URLDecoder.decode(bearerToken.substring(7), StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.debug("[Auth][BE40101] token decoding failed", e);
                throw new UnauthorizedException();
            }
        }
        return null;
    }
}
