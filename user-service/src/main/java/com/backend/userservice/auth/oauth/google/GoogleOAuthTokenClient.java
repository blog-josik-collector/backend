package com.backend.userservice.auth.oauth.google;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * 테스트/연동용: authorization code를 Google에 보내 access_token, id_token 등을 받습니다.
 * <p>
 * {@code code}는 한 번만 사용 가능하며 짧은 시간 내에 교환해야 합니다.
 */
@Component
public class GoogleOAuthTokenClient {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleOAuthTokenClient(
            RestClient.Builder restClientBuilder,
            @Value("${google.oauth2.client-id}") String clientId,
            @Value("${google.oauth2.client-secret}") String clientSecret,
            @Value("${google.oauth2.redirect-uri}") String redirectUri) {
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /**
     * Authorization code를 access_token / id_token 등으로 교환합니다.
     *
     * @param authorizationCode 인가 요청 후 콜백 쿼리의 {@code code} 값
     */
    public GoogleTokenResponse exchangeAuthorizationCode(String authorizationCode) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);

        return restClient.post()
                         .uri(TOKEN_URI)
                         .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                         .body(form)
                         .retrieve()
                         .body(GoogleTokenResponse.class);
    }
}
