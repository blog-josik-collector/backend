package com.backend.commondataaccess.security;

import com.backend.commondataaccess.exception.UnauthorizedException;
import java.util.Objects;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 역할: {@link CurrentUser} 가 붙은 파라미터에 SecurityContext 의 {@link JwtPrincipal} 을 주입한다. <p> 책임 <p> - principal 타입 확인 <p> -
 * required=true 인데 인증 정보가 없으면 {@link UnauthorizedException} 으로 401 응답 유도 <p> 비책임 <p> - 토큰 파싱/검증(JwtAuthenticationFilter 책임) <p> - URL 단위 접근
 * 제어(SecurityConfig 책임) <p>
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && JwtPrincipal.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = (authentication != null) ? authentication.getPrincipal() : null;

        if (principal instanceof JwtPrincipal jwtPrincipal) {
            return jwtPrincipal;
        }

        if (Objects.requireNonNull(parameter.getParameterAnnotation(CurrentUser.class)).required()) {
            throw new UnauthorizedException();
        }

        return null;
    }
}
