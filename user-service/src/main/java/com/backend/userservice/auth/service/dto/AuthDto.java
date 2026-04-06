package com.backend.userservice.auth.service.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class AuthDto {

    // request
    private String userId;
    private String password;

    // response
    private String accessToken;

    public static AuthDto from(String accessToken) {
        return AuthDto.builder()
                      .accessToken(accessToken)
                      .build();
    }

    public static AuthDto of(String userId, String password) {
        return AuthDto.builder()
                      .userId(userId)
                      .password(password)
                      .build();
    }

}
