package com.backend.userservice.user.service.dto;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.LoginType;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(fluent = true)
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더용
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // Jackson 역직렬화용
public class UserDto {

    // only request
    private String loginId;
    private String password;
    private String passwordConfirm;
    private String nickname;

    // response
    private UUID userId;
    private LoginType loginType;
    private UserType userType;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime lastLoginAt;

    public static UserDto from(User user) {
        return UserDto.builder()
                      .userId(user.id())
                      .nickname(user.nickname())
                      .userType(user.userType())
                      .createdAt(user.createdAt())
                      .updatedAt(user.updatedAt())
                      .lastLoginAt(user.lastLoginAt())
                      .build();
    }

    public static UserDto of(UUID userId, String nickname) {
        return UserDto.builder()
                      .userId(userId)
                      .nickname(nickname)
                      .build();
    }

    public static UserDto of(String loginId, String password) {
        return UserDto.builder()
                      .loginId(loginId)
                      .nickname(password)
                      .build();
    }

    public static UserDto of(String loginId, String password, String passwordConfirm, String nickname) {
        return UserDto.builder()
                      .loginId(loginId)
                      .password(password)
                      .passwordConfirm(passwordConfirm)
                      .nickname(nickname)
                      .build();
    }
}
