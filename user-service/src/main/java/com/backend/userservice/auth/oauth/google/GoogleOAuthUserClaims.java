package com.backend.userservice.auth.oauth.google;

/**
 * Google {@code id_token} 검증 후 서명 검증이 끝난 사용자 클레임.
 * <p>
 * DB 매핑·연동에는 {@code subject}({@code sub})를 우선으로 쓰는 것을 권장합니다.
 */
public record GoogleOAuthUserClaims(String subject,
                                    String email,
                                    boolean emailVerified,
                                    String name,
                                    String picture) {

}
