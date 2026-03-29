package com.backend.userservice.user.controller.dto;

import com.backend.commondb.user.enums.LoginType;
import com.backend.commondb.user.enums.UserType;
import com.backend.userservice.user.service.dto.UserDto;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserReadDto {

    public static class Request {


    }

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {

        private String id;
        private UserType userType;
        private LoginType loginType;
        private String userId;
        private String nickname;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
        private OffsetDateTime lastLoginAt;

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
