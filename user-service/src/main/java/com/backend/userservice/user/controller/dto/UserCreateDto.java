package com.backend.userservice.user.controller.dto;

import com.backend.userservice.user.service.dto.UserDto;
import java.time.OffsetDateTime;
import lombok.Builder;

// 네임스페이스 역할을 하는 외부 Record
public record UserCreateDto() {

    public record Request(String userId,
                          String password,
                          String nickname
    ) {

    }

    @Builder
    public record Response(String id,
                           OffsetDateTime createdAt) {

        public static Response from(UserDto userDto) {
            return Response.builder()
                           .id(userDto.id().toString())
                           .createdAt(userDto.createdAt())
                           .build();
        }
    }
}
