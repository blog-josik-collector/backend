package com.backend.userservice.user.bootstrap;

import com.backend.commondataaccess.exception.StateConflictException;
import com.backend.userservice.user.service.UserService;
import com.backend.userservice.user.service.dto.UserDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 활성 ADMIN 계정이 없을 때만 초기 운영자 계정을 생성한다. <br> {@code app.bootstrap.admin.enabled=true} 일 때만 활성. <br> 비밀번호는 평문 env 로 받아 {@link UserService#createAdmin} → BCrypt 저장 경로를 그대로 사용한다. (HTTP 로그인 API의 Base64
 * 인코딩은 전송 계층 규약이며, bootstrap 입력과는 무관하다.)
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.bootstrap.admin", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapProperties properties;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        if (userService.hasAdmin()) {
            log.info("[AdminBootstrap] skipped: active ADMIN already exists");
            return;
        }

        validateProperties();

        String loginId = properties.loginId();
        String nickname = StringUtils.defaultIfBlank(properties.nickname(), loginId);
        String password = properties.password();

        try {
            userService.createAdmin(UserDto.of(loginId, password, password, nickname));
            log.info("[AdminBootstrap] created ADMIN loginId={} nickname={}", loginId, nickname);
        } catch (StateConflictException e) {
            // 다중 인스턴스 동시 기동 등으로 이미 생성된 경우
            log.info("[AdminBootstrap] skipped after race: {}", e.getMessage());
        }
    }

    private void validateProperties() {
        if (StringUtils.isBlank(properties.loginId())) {
            throw new IllegalStateException(
                    "app.bootstrap.admin.enabled=true 인데 login-id(ADMIN_LOGIN_ID)가 비어 있습니다.");
        }
        if (StringUtils.isBlank(properties.password())) {
            throw new IllegalStateException(
                    "app.bootstrap.admin.enabled=true 인데 password(ADMIN_PASSWORD)가 비어 있습니다. "
                            + "시크릿/환경변수로 평문 비밀번호를 주입하세요.");
        }
    }
}
