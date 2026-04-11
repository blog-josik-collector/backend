package com.backend.userservice.user.controller.dto;

import com.backend.userservice.user.service.dto.UserDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record UserUpdateDto() {

    public record Request(String nickname) {

    }

    public record PasswordRequest(String password,
                                  String newPassword) {

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
