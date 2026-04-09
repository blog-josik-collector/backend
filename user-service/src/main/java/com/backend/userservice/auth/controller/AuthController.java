package com.backend.userservice.auth.controller;

import com.backend.userservice.auth.controller.dto.LoginDto;
import com.backend.userservice.auth.controller.dto.LoginDto.LoginResponse;
import com.backend.userservice.auth.controller.dto.LoginDto.PasswordRequest;
import com.backend.userservice.auth.oauth.google.GoogleIdTokenVerifierService;
import com.backend.userservice.auth.oauth.google.GoogleOAuthTokenClient;
import com.backend.userservice.auth.oauth.google.GoogleOAuthUserClaims;
import com.backend.userservice.auth.oauth.google.GoogleTokenResponse;
import com.backend.userservice.auth.service.AuthService;
import com.backend.userservice.auth.service.dto.AuthDto;
import com.backend.userservice.auth.service.dto.AuthDto.GoogleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "02. 로그인 관련 API")
@RequestMapping(value = "/auth/v1")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleOAuthTokenClient googleOAuthTokenClient;
    private final GoogleIdTokenVerifierService googleIdTokenVerifierService;

    @Operation(summary = "직접 가입한 계정으로 로그인")
    @PostMapping("/auth/login")
    public ResponseEntity<LoginDto.LoginResponse> func(@RequestBody PasswordRequest loginRequest) {
        AuthDto.PasswordRequest passwordRequest = AuthDto.PasswordRequest.of(loginRequest.userId(), loginRequest.password());
        AuthDto.Response login = authService.loginWithPassword(passwordRequest);
        LoginResponse response = LoginResponse.from(login);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "SNS 계정으로 로그인(Google 콜백 — code 교환 후 id_token 검증·파싱)")
    @GetMapping("/oauth/google/callback")
    public ResponseEntity<LoginDto.LoginResponse> oauthGoogleCallback(@RequestParam("code") String authorizationCode) {
        log.info("authorization code received");

        GoogleTokenResponse token = googleOAuthTokenClient.exchangeAuthorizationCode(authorizationCode);
        GoogleOAuthUserClaims claims = googleIdTokenVerifierService.verifyAndParse(token.idToken());
        log.info("Google id_token verified; subject={}, emailVerified={}", claims.subject(), claims.emailVerified());

        AuthDto.Response login = authService.loginWithGoogle(GoogleRequest.from(claims));

        LoginResponse response = LoginResponse.from(login);

        return ResponseEntity.ok(response);
    }
}
