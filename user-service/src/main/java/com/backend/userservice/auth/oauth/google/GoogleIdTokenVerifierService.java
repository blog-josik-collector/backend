package com.backend.userservice.auth.oauth.google;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.ErrorCode;
import com.backend.commondataaccess.exception.InfraException;
import com.backend.commondataaccess.exception.UnauthorizedException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Google {@code id_token}(OpenID Connect JWT)을 서명·검증하고 클레임을 추출합니다.
 * <p>
 * 우리 서비스 {@code JwtService}와는 별개이며, Google의 JWKS로 RS256 서명을 검증합니다.
 */
@Slf4j
@Component
public class GoogleIdTokenVerifierService {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdTokenVerifierService(@Value("${google.oauth2.client-id}") String clientId) {
        if (!StringUtils.hasText(clientId)) {
            throw new InfraException(ErrorCode.IE_GOOGLE_AUTH_ERROR, "google.oauth2.client-id is required for Google id_token verification");
        }
        try {
            this.verifier = new GoogleIdTokenVerifier.Builder(
                    GoogleNetHttpTransport.newTrustedTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId))
                    .build();
        } catch (GeneralSecurityException | IOException e) {
            log.error("[Auth][IE50004] failed to initialize Google id_token verifier", e);
            throw new InfraException(ErrorCode.IE_GOOGLE_AUTH_ERROR, "Failed to initialize GoogleIdTokenVerifier", e);
        }
    }

    /**
     * Google이 발급한 {@code id_token} 문자열을 검증합니다.
     *
     * @param idTokenString JWT 문자열 (세 부분을 점으로 이은 값)
     * @return 검증된 사용자 클레임
     * @throws BadRequestException 토큰이 없거나 비어 있음
     * @throws UnauthorizedException 검증 실패
     * @throws InfraException SDK 에러 시
     */
    public GoogleOAuthUserClaims verifyAndParse(String idTokenString) {
        if (!StringUtils.hasText(idTokenString)) {
            log.debug("[Auth][BE40001] Google id_token missing");
            throw new BadRequestException("id_token is null or empty");
        }
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.warn("[Auth][BE40101] Google id_token verification failed");
                throw new UnauthorizedException("Invalid Google id_token: verification returned null");
            }
            GoogleIdToken.Payload payload = idToken.getPayload();
            String subject = payload.getSubject();
            String email = payload.getEmail();
            Boolean emailVerified = payload.getEmailVerified();
            String name = payload.get("name") != null ? String.valueOf(payload.get("name")) : null;
            String picture = payload.get("picture") != null ? String.valueOf(payload.get("picture")) : null;

            return new GoogleOAuthUserClaims(
                    subject,
                    email,
                    emailVerified != null && emailVerified,
                    name,
                    picture
            );
        } catch (IOException | GeneralSecurityException e) {
            log.error("[Auth][IE50004] Google id_token verification error", e);
            throw new InfraException(ErrorCode.IE_GOOGLE_AUTH_ERROR, "Failed to verify Google id_token", e);
        }
    }
}
