package com.backend.userservice.auth.oauth.google;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.InfraException;
import com.backend.commondataaccess.exception.UnauthorizedException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import java.io.IOException;
import java.security.GeneralSecurityException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("GoogleIdTokenVerifierService 테스트")
@ExtendWith(MockitoExtension.class)
class GoogleIdTokenVerifierServiceTest {

    private static final String CLIENT_ID = "test_client_id";

    @Mock
    private GoogleIdTokenVerifier verifier;

    @Mock
    private GoogleIdToken idToken;

    private GoogleIdTokenVerifierService googleIdTokenVerifierService;

    @BeforeEach
    void init() {
        googleIdTokenVerifierService = new GoogleIdTokenVerifierService(CLIENT_ID);
        ReflectionTestUtils.setField(googleIdTokenVerifierService, "verifier", verifier);
    }

    @DisplayName("생성자 테스트")
    @Nested
    class ConstructorTest {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void client_id가_비어있으면_InfraException이_발생한다(String clientId) {
            Assertions.assertThatThrownBy(() -> new GoogleIdTokenVerifierService(clientId))
                      .isInstanceOf(InfraException.class);
        }

        @Test
        void verifier_초기화에_실패하면_InfraException이_발생한다() {
            try (MockedStatic<GoogleNetHttpTransport> transport = Mockito.mockStatic(GoogleNetHttpTransport.class)) {
                transport.when(GoogleNetHttpTransport::newTrustedTransport)
                         .thenThrow(new GeneralSecurityException("test_error"));

                Assertions.assertThatThrownBy(() -> new GoogleIdTokenVerifierService(CLIENT_ID))
                          .isInstanceOf(InfraException.class)
                          .hasCauseInstanceOf(GeneralSecurityException.class);
            }
        }
    }

    @DisplayName("verifyAndParse 테스트")
    @Nested
    class VerifyAndParseTest {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" "})
        void id_token이_비어있으면_BadRequestException이_발생한다(String idTokenString) {
            Assertions.assertThatThrownBy(() -> googleIdTokenVerifierService.verifyAndParse(idTokenString))
                      .isInstanceOf(BadRequestException.class);
        }

        @Test
        void 검증_결과가_null이면_UnauthorizedException이_발생한다() throws Exception {
            // given
            String idTokenString = "invalid.id.token";

            Mockito.doReturn(null).when(verifier).verify(idTokenString);

            // when & then
            Assertions.assertThatThrownBy(() -> googleIdTokenVerifierService.verifyAndParse(idTokenString))
                      .isInstanceOf(UnauthorizedException.class);
        }

        @Test
        void 검증에_성공하면_사용자_클레임을_반환한다() throws Exception {
            // given
            String idTokenString = "valid.id.token";

            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject("test_subject");
            payload.setEmail("test_email");
            payload.setEmailVerified(true);
            payload.set("name", "test_name");
            payload.set("picture", "test_picture_url");

            Mockito.doReturn(idToken).when(verifier).verify(idTokenString);
            Mockito.doReturn(payload).when(idToken).getPayload();

            // when
            GoogleOAuthUserClaims claims = googleIdTokenVerifierService.verifyAndParse(idTokenString);

            // then
            Assertions.assertThat(claims.subject()).isEqualTo("test_subject");
            Assertions.assertThat(claims.email()).isEqualTo("test_email");
            Assertions.assertThat(claims.emailVerified()).isTrue();
            Assertions.assertThat(claims.name()).isEqualTo("test_name");
            Assertions.assertThat(claims.picture()).isEqualTo("test_picture_url");
        }

        @Test
        void 선택_클레임이_없으면_null과_미인증으로_반환한다() throws Exception {
            // given
            String idTokenString = "valid.id.token";

            GoogleIdToken.Payload payload = new GoogleIdToken.Payload();
            payload.setSubject("test_subject");

            Mockito.doReturn(idToken).when(verifier).verify(idTokenString);
            Mockito.doReturn(payload).when(idToken).getPayload();

            // when
            GoogleOAuthUserClaims claims = googleIdTokenVerifierService.verifyAndParse(idTokenString);

            // then
            Assertions.assertThat(claims.subject()).isEqualTo("test_subject");
            Assertions.assertThat(claims.email()).isNull();
            Assertions.assertThat(claims.emailVerified()).isFalse();
            Assertions.assertThat(claims.name()).isNull();
            Assertions.assertThat(claims.picture()).isNull();
        }

        @Test
        void 검증_중_SDK_에러가_발생하면_InfraException이_발생한다() throws Exception {
            // given
            String idTokenString = "valid.id.token";

            Mockito.doThrow(new IOException("test_error")).when(verifier).verify(idTokenString);

            // when & then
            Assertions.assertThatThrownBy(() -> googleIdTokenVerifierService.verifyAndParse(idTokenString))
                      .isInstanceOf(InfraException.class)
                      .hasCauseInstanceOf(IOException.class);
        }
    }
}
