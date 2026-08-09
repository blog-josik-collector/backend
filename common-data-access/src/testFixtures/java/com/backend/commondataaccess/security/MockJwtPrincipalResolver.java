package com.backend.commondataaccess.security;

import com.backend.commondataaccess.persistence.user.enums.UserType;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * standaloneSetup MockMvc 는 시큐리티 필터를 거치지 않으므로 principal 을 대신 주입한다. <p>
 * {@link #AUTHENTICATION_ID} 와 {@link #USER_ID} 는 서로 다른 고정값이라, 두 필드를 혼동하면 테스트가 실패한다.
 */
public class MockJwtPrincipalResolver implements HandlerMethodArgumentResolver {

    public static final UUID AUTHENTICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final String NICKNAME = "test_nickname";

    private final JwtPrincipal principal;

    public MockJwtPrincipalResolver() {
        this(UserType.USER);
    }

    public MockJwtPrincipalResolver(UserType userType) {
        this.principal = JwtPrincipal.builder()
                                     .id(AUTHENTICATION_ID)
                                     .userId(USER_ID)
                                     .nickname(NICKNAME)
                                     .roles(new String[]{userType.name()})
                                     .build();
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && JwtPrincipal.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        return principal;
    }
}
