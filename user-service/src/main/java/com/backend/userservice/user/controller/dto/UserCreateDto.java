package com.backend.userservice.user.controller.dto;

import com.backend.userservice.user.service.dto.UserDto;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

// 네임스페이스 역할을 하는 외부 Record
public record UserCreateDto() {

    public record Request(String loginId,
                          String password,
                          String passwordConfirm,
                          String nickname
    ) {

        public String getDecodedPassword() {
            if (StringUtils.isBlank(this.password)) {
                return null;
            }
            return new String(Base64.getDecoder().decode(this.password), StandardCharsets.UTF_8);
        }

        public String getDecodedPasswordConfirm() {
            if (StringUtils.isBlank(this.passwordConfirm)) {
                return null;
            }
            return new String(Base64.getDecoder().decode(this.passwordConfirm), StandardCharsets.UTF_8);
        }

    }

    @Builder
    public record Response(String userId,
                           OffsetDateTime createdAt) {

        public static Response from(UserDto userDto) {
            return Response.builder()
                           .userId(userDto.userId().toString())
                           .createdAt(userDto.createdAt())
                           .build();
        }
    }
}
