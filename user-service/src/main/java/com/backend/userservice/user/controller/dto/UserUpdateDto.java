package com.backend.userservice.user.controller.dto;

import com.backend.userservice.user.service.dto.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

public record UserUpdateDto() {

    @Schema(description = "회원정보 수정 요청")
    public record Request(
            @Schema(description = "변경할 닉네임", example = "new_nickname")
            String nickname) {

    }

    @Schema(description = "비밀번호 변경 요청")
    public record PasswordRequest(
            @Schema(description = "Base64 인코딩된 현재 비밀번호", example = "cGFzc3dvcmQ=")
            String password,

            @Schema(description = "Base64 인코딩된 새 비밀번호", example = "bmV3cGFzc3dvcmQ=")
            String newPassword) {

        public String getDecodedPassword() {
            if (StringUtils.isBlank(this.password)) {
                return null;
            }
            return new String(Base64.getDecoder().decode(this.password), StandardCharsets.UTF_8);
        }

        public String getDecodedNewPassword() {
            if (StringUtils.isBlank(this.newPassword)) {
                return null;
            }
            return new String(Base64.getDecoder().decode(this.newPassword), StandardCharsets.UTF_8);
        }
    }

    @Schema(description = "회원정보/비밀번호 수정 결과")
    @Builder
    public record Response(
            @Schema(description = "사용자 ID")
            UUID userId,

            @Schema(description = "수정 시각")
            OffsetDateTime updatedAt) {

        public static Response from(UserDto userDto) {
            return Response.builder()
                           .userId(userDto.userId())
                           .updatedAt(userDto.updatedAt())
                           .build();
        }

    }
}
