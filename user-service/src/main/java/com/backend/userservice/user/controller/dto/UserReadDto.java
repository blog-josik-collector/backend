package com.backend.userservice.user.controller.dto;

import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.userservice.user.service.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import lombok.Builder;

// 네임스페이스 역할을 하는 외부 Record
public record UserReadDto() {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record Response(String userId,
                           UserType userType,
                           String nickname,
                           OffsetDateTime createdAt,
                           OffsetDateTime updatedAt,
                           OffsetDateTime lastLoginAt) {

        public static Response from(UserDto userDto) {
            return Response.builder()
                           .userId(userDto.userId().toString())
                           .userType(userDto.userType())
                           .nickname(userDto.nickname())
                           .createdAt(userDto.createdAt())
                           .updatedAt(userDto.updatedAt())
                           .lastLoginAt(userDto.lastLoginAt())
                           .build();
        }
    }
}
