package com.backend.userservice.user.controller.dto;

import com.backend.commondataaccess.persistence.user.enums.LoginType;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.userservice.user.service.dto.UserDto;
import java.time.OffsetDateTime;
import lombok.Builder;

// 네임스페이스 역할을 하는 외부 Record
public record UserReadDto() {

    public record Request() {

    }

    @Builder
    public record Response(String id,
                           UserType userType,
                           LoginType loginType,
                           String userId,
                           String nickname,
                           OffsetDateTime createdAt,
                           OffsetDateTime updatedAt,
                           OffsetDateTime lastLoginAt) {

        public static Response from(UserDto userDto) {
            return Response.builder()
                           .id(userDto.id().toString())
                           .userType(userDto.userType())
                           .loginType(userDto.loginType())
                           .userId(userDto.userId())
                           .nickname(userDto.nickname())
                           .createdAt(userDto.createdAt())
                           .updatedAt(userDto.updatedAt())
                           .lastLoginAt(userDto.lastLoginAt())
                           .build();
        }
    }
}
