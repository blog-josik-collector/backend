package com.backend.userservice.user.controller.dto;

import com.backend.userservice.user.service.dto.UserDto;
import java.time.OffsetDateTime;
import lombok.Builder;

// 네임스페이스 역할을 하는 외부 Record
public record UserCreateDto() {

    public record Request(String loginId,
                          String password,
                          String passwordConfirm,
                          String nickname
    ) {

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
