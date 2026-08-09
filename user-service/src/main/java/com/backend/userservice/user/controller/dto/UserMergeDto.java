package com.backend.userservice.user.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserMergeDto() {

    @Schema(description = "OAuth 계정 통합 요청")
    public record Request(
            @Schema(description = "통합할 OAuth 액세스 토큰")
            String accessToken) {

    }
}
