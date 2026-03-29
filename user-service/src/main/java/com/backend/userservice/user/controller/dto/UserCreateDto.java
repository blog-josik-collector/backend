package com.backend.userservice.user.controller.dto;

import com.backend.userservice.user.service.dto.UserDto;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserCreateDto {

    @Getter
    @NoArgsConstructor
    public static class Request {
        String userId;
        String password;
        String nickname;
    }

    @Builder
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        String id;
        OffsetDateTime createdAt;

        public static Response from(UserDto userDto) {
            return Response.builder()
                           .id(userDto.id().toString())
                           .createdAt(userDto.createdAt())
                           .build();
        }
    }
}
