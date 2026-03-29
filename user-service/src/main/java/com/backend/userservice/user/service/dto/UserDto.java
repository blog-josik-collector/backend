package com.backend.userservice.user.service.dto;

import com.backend.commondb.user.enums.LoginType;
import com.backend.commondb.user.User;
import com.backend.commondb.user.enums.UserType;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDto {

    // only request
    private String userId;
    private String password;
    private String nickname;

    // response
    private UUID id;
    private LoginType loginType;
    private UserType userType;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime lastLoginAt;

    public static UserDto of(String userId, String password, String nickname) {
        return UserDto.builder()
                      .userId(userId)
                      .password(password)
                      .nickname(nickname)
                      .build();
    }

    public static UserDto from(User user) {
        return UserDto.builder()
                      .id(user.id())
                      .userId(user.userId())
                      .nickname(user.nickname())
                      .loginType(user.loginType())
                      .userType(user.userType())
                      .createdAt(user.createdAt())
                      .updatedAt(user.updatedAt())
                      .lastLoginAt(user.lastLoginAt())
                      .build();
    }
}
