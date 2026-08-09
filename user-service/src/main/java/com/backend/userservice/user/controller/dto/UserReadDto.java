package com.backend.userservice.user.controller.dto;

import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.userservice.user.service.dto.UserDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import lombok.Builder;

// 네임스페이스 역할을 하는 외부 Record
public record UserReadDto() {

    @Schema(description = "회원정보 조회 결과")
    @Builder
    public record Response(
            @Schema(description = "사용자 ID")
            String userId,

            @Schema(description = "사용자 유형")
            UserType userType,

            @Schema(description = "닉네임", example = "cycy")
            String nickname,

            @Schema(description = "가입 시각")
            OffsetDateTime createdAt,

            @Schema(description = "최종 수정 시각")
            OffsetDateTime updatedAt,

            @Schema(description = "마지막 로그인 시각")
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
