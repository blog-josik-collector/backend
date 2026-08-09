package com.backend.userservice.user.bootstrap;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 최초 기동 시 운영자(ADMIN) 계정 부트스트랩 설정. <br>
 * 비밀번호는 환경변수/시크릿으로만 주입한다(레포에 실비밀번호를 커밋하지 않는다). <br>
 * API 로그인 시 클라이언트는 이 평문 비밀번호를 Base64로 인코딩해 전송한다.
 */
@ConfigurationProperties(prefix = "app.bootstrap.admin")
public record AdminBootstrapProperties(boolean enabled,
                                       String loginId,
                                       String password,
                                       String nickname) {
}
