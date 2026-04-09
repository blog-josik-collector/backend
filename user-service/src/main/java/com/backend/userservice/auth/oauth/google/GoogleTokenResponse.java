package com.backend.userservice.auth.oauth.google;

/**
 * Google OAuth2 token 엔드포인트(https://oauth2.googleapis.com/token) JSON 응답 매핑.
 * <p>
 * 애플리케이션의 {@code spring.jackson.property-naming-strategy: SNAKE_CASE} 설정에 맞춰
 * JSON의 snake_case 키가 필드에 매핑됩니다.
 */
public record GoogleTokenResponse(
        String accessToken,
        Long expiresIn,
        String refreshToken,
        String scope,
        String tokenType,
        String idToken
) {
}
