package com.backend.userservice.auth.oauth.google;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.backend.commondataaccess.exception.InfraException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@DisplayName("GoogleOAuthTokenClient 테스트")
class GoogleOAuthTokenClientTest {

    private static final String TOKEN_URI = "https://oauth2.googleapis.com/token";

    private MockRestServiceServer mockServer;

    private GoogleOAuthTokenClient googleOAuthTokenClient;

    @BeforeEach
    void init() {
        ObjectMapper snakeCaseObjectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

        RestClient.Builder restClientBuilder = RestClient.builder()
                                                         .messageConverters(converters -> converters.add(
                                                                 0, new MappingJackson2HttpMessageConverter(
                                                                         snakeCaseObjectMapper)));
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        googleOAuthTokenClient = new GoogleOAuthTokenClient(restClientBuilder,
                                                            "test_client_id",
                                                            "test_client_secret",
                                                            "http://localhost/callback");
    }

    @Test
    void authorization_code를_토큰으로_교환한다() {
        // given
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", "test_code");
        form.add("client_id", "test_client_id");
        form.add("client_secret", "test_client_secret");
        form.add("redirect_uri", "http://localhost/callback");

        String responseBody = """
                {
                  "access_token": "test_access_token",
                  "expires_in": 3599,
                  "scope": "openid email",
                  "token_type": "Bearer",
                  "id_token": "test_id_token"
                }
                """;

        mockServer.expect(requestTo(TOKEN_URI))
                  .andExpect(method(HttpMethod.POST))
                  .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_FORM_URLENCODED))
                  .andExpect(content().formData(form))
                  .andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

        // when
        GoogleTokenResponse response = googleOAuthTokenClient.exchangeAuthorizationCode("test_code");

        // then
        mockServer.verify();
        Assertions.assertThat(response.accessToken()).isEqualTo("test_access_token");
        Assertions.assertThat(response.expiresIn()).isEqualTo(3599L);
        Assertions.assertThat(response.refreshToken()).isNull();
        Assertions.assertThat(response.scope()).isEqualTo("openid email");
        Assertions.assertThat(response.tokenType()).isEqualTo("Bearer");
        Assertions.assertThat(response.idToken()).isEqualTo("test_id_token");
    }

    @Test
    void 토큰_교환에_실패하면_InfraException이_발생한다() {
        // given
        mockServer.expect(requestTo(TOKEN_URI))
                  .andExpect(method(HttpMethod.POST))
                  .andRespond(withServerError());

        // when & then
        Assertions.assertThatThrownBy(() -> googleOAuthTokenClient.exchangeAuthorizationCode("test_code"))
                  .isInstanceOf(InfraException.class);
        mockServer.verify();
    }
}
