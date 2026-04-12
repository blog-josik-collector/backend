package com.backend.commondataaccess.security;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class MockJwtPrincipalResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @AuthenticationPrincipal 어노테이션이 붙어 있고, 타입이 JwtPrincipal인 경우 작동
        return parameter.hasParameterAnnotation(AuthenticationPrincipal.class)
                && parameter.getParameterType().equals(JwtPrincipal.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        // 테스트에서 사용할 고정된 가짜 객체 반환
        User mockUser = User.builder()
                            .id(UUID.randomUUID())
                            .userType(UserType.USER)
                            .nickname("test_nickname")
                            .build();

        return JwtPrincipal.builder()
                           .id(UUID.randomUUID())
                           .userId(mockUser.id())
                           .nickname(mockUser.nickname())
                           .roles(new String[]{mockUser.userType().getValue()})
                           .build();
    }
}
