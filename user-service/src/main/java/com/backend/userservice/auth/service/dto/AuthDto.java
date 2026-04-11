package com.backend.userservice.auth.service.dto;

import com.backend.userservice.auth.oauth.google.GoogleOAuthUserClaims;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // Jackson 역직렬화용
public class AuthDto {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더용
    @NoArgsConstructor(access = AccessLevel.PRIVATE)  // Jackson 역직렬화용
    public static class PasswordRequest {

        private String loginId;
        private String password;

        public static AuthDto.PasswordRequest of(String userId, String password) {
            return AuthDto.PasswordRequest.builder()
                                          .loginId(userId)
                                          .password(password)
                                          .build();
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더용
    @NoArgsConstructor(access = AccessLevel.PRIVATE)  // Jackson 역직렬화용
    public static class GoogleRequest {

        private String subject; // sso_subject_id

        public static AuthDto.GoogleRequest from(GoogleOAuthUserClaims claims) {
            return GoogleRequest.builder()
                                .subject(claims.subject())
                                .build();
        }
    }

    // response
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Getter
    @Builder
    @AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더용
    @NoArgsConstructor(access = AccessLevel.PRIVATE)  // Jackson 역직렬화용
    public static class Response {

        private String accessToken;
        private String refreshToken;

        public static AuthDto.Response from(String accessToken) {
            return AuthDto.Response.builder()
                                   .accessToken(accessToken)
                                   .build();
        }

        public static AuthDto.Response of(String accessToken, String refreshToken) {
            return AuthDto.Response.builder()
                                   .accessToken(accessToken)
                                   .refreshToken(refreshToken)
                                   .build();
        }
    }
}
