package com.backend.userservice.auth.controller;

import com.backend.userservice.auth.controller.dto.LoginDto;
import com.backend.userservice.auth.controller.dto.LoginDto.LoginResponse;
import com.backend.userservice.auth.service.AuthService;
import com.backend.userservice.auth.service.dto.AuthDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "02. 로그인 관련 API")
@RequestMapping(value = "/auth/v1")
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "직접 가입한 계정으로 로그인")
    @PostMapping("/auth/login")
    public ResponseEntity<LoginDto.LoginResponse> func(@RequestBody LoginDto.DirectLoginRequest loginRequest) {
        AuthDto authDto = AuthDto.of(loginRequest.userId(), loginRequest.password());
        AuthDto login = authService.login(authDto);
        LoginResponse response = LoginResponse.from(login);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "SNS 계정으로 로그인")
    @PostMapping("/auth/oauth/login")
    public void func2() {

    }
}
