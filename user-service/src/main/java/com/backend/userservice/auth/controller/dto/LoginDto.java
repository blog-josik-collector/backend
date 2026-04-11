package com.backend.userservice.auth.controller.dto;

import com.backend.userservice.auth.service.dto.AuthDto;

// 네임스페이스 역할을 하는 외부 Record
public record LoginDto() {

    public record PasswordRequest(String loginId, String password) {

    }

    public record LoginResponse(String accessToken) {

        public static LoginResponse from(AuthDto.Response response) {
            return new LoginResponse(response.getAccessToken());
        }
    }
}
