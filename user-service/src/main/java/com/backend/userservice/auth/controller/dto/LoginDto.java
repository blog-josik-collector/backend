package com.backend.userservice.auth.controller.dto;

import com.backend.userservice.auth.service.dto.AuthDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;

// 네임스페이스 역할을 하는 외부 Record
public record LoginDto() {

    @Schema(description = "비밀번호 로그인 요청")
    public record PasswordRequest(
            @Schema(description = "로그인 ID", example = "user@example.com")
            String loginId,

            @Schema(description = "Base64 인코딩된 비밀번호", example = "cGFzc3dvcmQ=")
            String password) {

        // 로직에서 사용할 때는 디코딩된 값을 반환하는 별도 메서드 제공
        @Schema(hidden = true)
        public String getDecodedPassword() {
            if (StringUtils.isBlank(this.password)) {
                return null;
            }
            return new String(Base64.getDecoder().decode(this.password), StandardCharsets.UTF_8);
        }
    }

    @Schema(description = "로그인 응답")
    public record LoginResponse(
            @Schema(description = "JWT 액세스 토큰")
            String accessToken) {

        public static LoginResponse from(AuthDto.Response response) {
            return new LoginResponse(response.getAccessToken());
        }
    }
}
