package com.backend.userservice.user.controller.dto;

import com.backend.userservice.user.service.dto.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

// 네임스페이스 역할을 하는 외부 Record
public record UserCreateDto() {

    @Schema(description = "회원가입 요청")
    public record Request(
            @Schema(description = "로그인 ID", example = "user@example.com")
            String loginId,

            @Schema(description = "Base64 인코딩된 비밀번호", example = "cGFzc3dvcmQ=")
            String password,

            @Schema(description = "Base64 인코딩된 비밀번호 확인", example = "cGFzc3dvcmQ=")
            String passwordConfirm,

            @Schema(description = "닉네임", example = "cycy")
            String nickname
    ) {

        @Schema(hidden = true)
        public String getDecodedPassword() {
            if (StringUtils.isBlank(this.password)) {
                return null;
            }
            return new String(Base64.getDecoder().decode(this.password), StandardCharsets.UTF_8);
        }

        @Schema(hidden = true)
        public String getDecodedPasswordConfirm() {
            if (StringUtils.isBlank(this.passwordConfirm)) {
                return null;
            }
            return new String(Base64.getDecoder().decode(this.passwordConfirm), StandardCharsets.UTF_8);
        }

    }

    @Schema(description = "회원가입 결과")
    @Builder
    public record Response(
            @Schema(description = "생성된 사용자 ID")
            String userId,

            @Schema(description = "가입 시각")
            OffsetDateTime createdAt) {

        public static Response from(UserDto userDto) {
            return Response.builder()
                           .userId(userDto.userId().toString())
                           .createdAt(userDto.createdAt())
                           .build();
        }
    }
}
