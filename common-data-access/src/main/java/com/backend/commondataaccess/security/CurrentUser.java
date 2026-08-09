package com.backend.commondataaccess.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 역할: 인증된 사용자({@link JwtPrincipal})를 컨트롤러 파라미터로 주입받는다. <p> 책임 <p> - 인증 필수 여부를 시그니처에 드러내기 <p> - 미인증 요청 처리 규칙을
 * {@link CurrentUserArgumentResolver} 한 곳으로 모으기 <p> 주의점 <p> - Spring Security 의 {@code @AuthenticationPrincipal} 을 메타 애노테이션으로 붙이면
 * {@code AuthenticationPrincipalArgumentResolver} 가 먼저 처리해 {@link #required()} 검증이 무시될 수 있다. 그래서 독립 애노테이션으로 둔다. <p>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CurrentUser {

    /**
     * true 이면 미인증 요청에 {@link com.backend.commondataaccess.exception.UnauthorizedException} 을 던진다. <p> false 는 공개 API 가 로그인 사용자에게만
     * 부가 정보를 채워줄 때만 사용한다. 이 경우 파라미터로 null 이 들어올 수 있다.
     */
    boolean required() default true;
}
