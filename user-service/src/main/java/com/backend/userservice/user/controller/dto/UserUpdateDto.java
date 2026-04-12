package com.backend.userservice.user.controller.dto;

import com.backend.userservice.user.service.dto.UserDto;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

public record UserUpdateDto() {

    public record Request(String nickname) {

    }

    public record PasswordRequest(String password,
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

    @Builder
    public record Response(UUID userId,
                           OffsetDateTime updatedAt) {

        public static Response from(UserDto userDto) {
            return Response.builder()
                           .userId(userDto.userId())
                           .updatedAt(userDto.updatedAt())
                           .build();
        }

    }
}
