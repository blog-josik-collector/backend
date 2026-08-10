package com.backend.commondataaccess.security;

import com.backend.commondataaccess.persistence.user.enums.UserType;
import java.util.Objects;
import java.util.UUID;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * MockMvc {@code standaloneSetup} 은 시큐리티 필터를 거치지 않으므로 {@link CurrentUser} principal 을 대신 주입한다.
 * <p>
 * {@link #AUTHENTICATION_ID} 와 {@link #USER_ID} 는 서로 다른 고정값이라, 두 필드를 혼동하면 테스트가 실패한다.
 * <p>
 * 기본은 {@link UserType#USER}. 관리자 API 시나리오는 {@link #admin()} 또는
 * {@code new MockJwtPrincipalResolver(UserType.ADMIN)} 을 사용한다.
 * <p>
 * Phase 0 헬퍼 전략: {@code common-data-access} testFixtures 에 유지(옵션 A).
 * 엔티티 공용 빌더·anonymous({@code required=false}) resolver 는 필요할 때 추가한다.
 */
public class MockJwtPrincipalResolver implements HandlerMethodArgumentResolver {

    public static final UUID AUTHENTICATION_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    public static final String NICKNAME = "test_nickname";

    private final JwtPrincipal principal;

    /** 일반 사용자 principal (기본). */
    public static MockJwtPrincipalResolver user() {
        return new MockJwtPrincipalResolver(UserType.USER);
    }

    /** 관리자 principal (integrated-api admin 시나리오 등). */
    public static MockJwtPrincipalResolver admin() {
        return new MockJwtPrincipalResolver(UserType.ADMIN);
    }

    public MockJwtPrincipalResolver() {
        this(UserType.USER);
    }

    public MockJwtPrincipalResolver(UserType userType) {
        Objects.requireNonNull(userType, "userType");
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
