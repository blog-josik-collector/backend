package com.backend.userservice.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.security.MockJwtPrincipalResolver;
import com.backend.userservice.auth.controller.dto.LoginDto;
import com.backend.userservice.auth.oauth.google.GoogleIdTokenVerifierService;
import com.backend.userservice.auth.oauth.google.GoogleOAuthTokenClient;
import com.backend.userservice.auth.oauth.google.GoogleOAuthUserClaims;
import com.backend.userservice.auth.oauth.google.GoogleTokenResponse;
import com.backend.userservice.auth.service.AuthService;
import com.backend.userservice.auth.service.dto.AuthDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("AuthController 테스트")
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private AuthController authController;

    @Mock
    private AuthService authService;

    @Mock
    private GoogleOAuthTokenClient googleOAuthTokenClient;

    @Mock
    private GoogleIdTokenVerifierService googleIdTokenVerifierService;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController)
                                 .setCustomArgumentResolvers(new MockJwtPrincipalResolver())
                                 .build();
    }

    @Test
    void 직접_로그인을_한다() throws Exception {
        String loginId = "test_login_id";
        String password = "test_password";
        String testAccessToken = "test_access_token";

        LoginDto.PasswordRequest request = new LoginDto.PasswordRequest(loginId, password);
        AuthDto.Response response = AuthDto.Response.from(testAccessToken);

        Mockito.doReturn(response).when(authService).loginWithPassword(any());

        mockMvc.perform(post("/auth/v1/auth/login")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.accessToken").value(response.getAccessToken()));
    }

    @Test
    void 구글_OAuth_로그인을_한다() throws Exception {
        String authorizationCode = "test_authorization_code";
        String testGoogleAccessToken = "test_google_access_token";
        String testGoogleRefreshToken = "test_google_refresh_token";
        String testScope = "test_scope";
        String testTokenType = "test_token_type";
        String testIdToken = "test_id_token";
        String testSubject = "test_subject";
        String testEmail = "test_email";
        String testName = "test_name";
        String testPictureUrl = "test_picture_url";
        String testAccessToken = "test_access_token";

        GoogleTokenResponse googleTokenResponse = new GoogleTokenResponse(testGoogleAccessToken,
                                                                          7200L,
                                                                          testGoogleRefreshToken,
                                                                          testScope,
                                                                          testTokenType,
                                                                          testIdToken);

        GoogleOAuthUserClaims googleOAuthUserClaims = new GoogleOAuthUserClaims(testSubject,
                                                                                testEmail,
                                                                                true,
                                                                                testName,
                                                                                testPictureUrl);

        AuthDto.Response response = AuthDto.Response.from(testAccessToken);

        Mockito.doReturn(googleTokenResponse).when(googleOAuthTokenClient).exchangeAuthorizationCode(any());
        Mockito.doReturn(googleOAuthUserClaims).when(googleIdTokenVerifierService).verifyAndParse(any());
        Mockito.doReturn(response).when(authService).loginWithGoogle(any());

        mockMvc.perform(get("/auth/v1/oauth/google/callback")
                                .param("code", authorizationCode)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.accessToken").value(response.getAccessToken()));
    }
}