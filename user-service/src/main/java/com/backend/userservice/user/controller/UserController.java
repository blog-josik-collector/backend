package com.backend.userservice.user.controller;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.userservice.user.controller.dto.UserCreateDto;
import com.backend.userservice.user.controller.dto.UserMergeDto;
import com.backend.userservice.user.controller.dto.UserReadDto;
import com.backend.userservice.user.controller.dto.UserUpdateDto;
import com.backend.userservice.user.controller.dto.UserUpdateDto.Response;
import com.backend.userservice.user.service.UserService;
import com.backend.userservice.user.service.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01. 회원정보 관련 API")
@RequestMapping(value = "/user/v1")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "직접 회원가입")
    @PostMapping("/users")
    public ResponseEntity<UserCreateDto.Response> create(@RequestBody UserCreateDto.Request request) {
        UserDto userDto = userService.create(UserDto.of(request.loginId(),
                                                        request.getDecodedPassword(),
                                                        request.getDecodedPasswordConfirm(),
                                                        request.nickname()));

        return ResponseEntity.ok(UserCreateDto.Response.from(userDto));
    }

    @Operation(summary = "회원정보 조회(내 정보 조회)")
    @GetMapping("/users/me")
    public ResponseEntity<UserReadDto.Response> getMe(@AuthenticationPrincipal JwtPrincipal authentication) {
        User user = userService.getUser(authentication.getUserId());
        UserDto userDto = UserDto.from(Objects.requireNonNull(user));
        return ResponseEntity.ok(UserReadDto.Response.from(userDto));
    }

    @Operation(summary = "회원정보 수정(내 정보 수정)")
    @PatchMapping("/users/me")
    public ResponseEntity<UserUpdateDto.Response> update(@AuthenticationPrincipal JwtPrincipal authentication,
                                                         @RequestBody UserUpdateDto.Request request) {

        UserDto userDto = UserDto.of(authentication.getUserId(), request.nickname());
        userService.update(userDto);
        return ResponseEntity.ok(Response.from(userService.getUserDto(userDto)));
    }

    @Operation(summary = "비밀번호 수정(내 정보 수정)")
    @PatchMapping("/users/me/password")
    public ResponseEntity<UserUpdateDto.Response> updatePassword(@AuthenticationPrincipal JwtPrincipal authentication,
                                                                 @RequestBody UserUpdateDto.PasswordRequest request) {

        userService.updatePassword(authentication.getUserId(), request.getDecodedPassword(), request.getDecodedNewPassword());
        return ResponseEntity.ok(Response.from(userService.getUserDto(authentication.getUserId())));
    }

    @Operation(summary = "회원정보 통합")
    @PostMapping("/users/me/merge-oauth")
    public ResponseEntity<Void> merge(@AuthenticationPrincipal JwtPrincipal authentication,
                                      @RequestBody UserMergeDto.Request request) {

        userService.merge(authentication.getId(), request.accessToken());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "회원탈퇴")
    @DeleteMapping("/users/me")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal JwtPrincipal authentication) {
        userService.delete(authentication.getUserId());
        return ResponseEntity.accepted().build();
    }
}
