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
    private String userId;
    private String password;
    private String passwordConfirm;
    private String nickname;

    // response
    private UUID id;
    private LoginType loginType;
    private UserType userType;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime lastLoginAt;

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

    public static UserDto of(UUID id, String nickname) {
        return UserDto.builder()
                      .id(id)
                      .nickname(nickname)
                      .build();
    }

    public static UserDto of(String userId, String password, String passwordConfirm, String nickname) {
        return UserDto.builder()
                      .userId(userId)
                      .password(password)
                      .passwordConfirm(passwordConfirm)
                      .nickname(nickname)
                      .build();
    }
}
