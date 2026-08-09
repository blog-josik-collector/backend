package com.backend.userservice.user.controller;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.security.CurrentUser;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01. 회원정보 관련 API")
@RequestMapping(value = "/user/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "직접 회원가입")
    @PostMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserCreateDto.Response> create(@RequestBody UserCreateDto.Request request) {
        UserDto userDto = userService.create(UserDto.of(request.loginId(),
                                                        request.getDecodedPassword(),
                                                        request.getDecodedPasswordConfirm(),
                                                        request.nickname()));

        return ResponseEntity.ok(UserCreateDto.Response.from(userDto));
    }

    @Operation(summary = "회원정보 조회(내 정보 조회)")
    @GetMapping("/users/me")
    public ResponseEntity<UserReadDto.Response> getMe(@CurrentUser JwtPrincipal principal) {
        User user = userService.getUser(principal.getUserId());
        UserDto userDto = UserDto.from(user);
        return ResponseEntity.ok(UserReadDto.Response.from(userDto));
    }

    @Operation(summary = "회원정보 수정(내 정보 수정)")
    @PatchMapping(value = "/users/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserUpdateDto.Response> update(@CurrentUser JwtPrincipal principal,
                                                         @RequestBody UserUpdateDto.Request request) {

        UserDto userDto = UserDto.of(principal.getUserId(), request.nickname());
        userService.update(userDto);
        return ResponseEntity.ok(Response.from(userService.getUserDto(userDto)));
    }

    @Operation(summary = "비밀번호 수정(내 정보 수정)")
    @PatchMapping(value = "/users/me/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserUpdateDto.Response> updatePassword(@CurrentUser JwtPrincipal principal,
                                                                 @RequestBody UserUpdateDto.PasswordRequest request) {

        userService.updatePassword(principal.getUserId(), request.getDecodedPassword(), request.getDecodedNewPassword());
        return ResponseEntity.ok(Response.from(userService.getUserDto(principal.getUserId())));
    }

    @Operation(summary = "회원정보 통합")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PostMapping(value = "/users/me/merge-oauth", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> merge(@CurrentUser JwtPrincipal principal,
                                      @RequestBody UserMergeDto.Request request) {

        userService.merge(principal.getId(), request.accessToken());
        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "회원탈퇴")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @DeleteMapping("/users/me")
    public ResponseEntity<Void> delete(@CurrentUser JwtPrincipal principal) {
        userService.delete(principal.getUserId());
        return ResponseEntity.accepted().build();
    }
}
