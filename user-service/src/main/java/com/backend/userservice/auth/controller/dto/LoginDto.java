package com.backend.userservice.auth.controller.dto;

import com.backend.userservice.auth.service.dto.AuthDto;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.lang3.StringUtils;

// 네임스페이스 역할을 하는 외부 Record
public record LoginDto() {

    public record PasswordRequest(String loginId, String password) {

        // 로직에서 사용할 때는 디코딩된 값을 반환하는 별도 메서드 제공
        public String getDecodedPassword() {
            if (StringUtils.isBlank(this.password)) {
                return null;
            }
            return new String(Base64.getDecoder().decode(this.password), StandardCharsets.UTF_8);
        }
    }

    public record LoginResponse(String accessToken) {

        public static LoginResponse from(AuthDto.Response response) {
            return new LoginResponse(response.getAccessToken());
        }
    }
}
