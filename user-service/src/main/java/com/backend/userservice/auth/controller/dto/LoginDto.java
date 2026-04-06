package com.backend.userservice.auth.controller.dto;

import com.backend.userservice.auth.service.dto.AuthDto;
import lombok.Builder;

// 네임스페이스 역할을 하는 외부 Record
public record LoginDto() {

    public record DirectLoginRequest(String userId, String password) {

    }

    public record OAuth2LoginRequest(String userId, String password) {

    }

    @Builder
    public record LoginResponse(String accessToken) {

        public static LoginResponse from(AuthDto authDto) {
            return LoginResponse.builder()
                                .accessToken(authDto.accessToken())
                                .build();
        }
    }
}
