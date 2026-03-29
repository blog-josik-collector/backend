package com.backend.userservice.user.controller;

import com.backend.userservice.user.controller.dto.UserCreateDto;
import com.backend.userservice.user.controller.dto.UserDeleteDto;
import com.backend.userservice.user.controller.dto.UserMergeDto;
import com.backend.userservice.user.controller.dto.UserReadDto;
import com.backend.userservice.user.controller.dto.UserUpdateDto;
import com.backend.userservice.user.service.UserService;
import com.backend.userservice.user.service.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RequestMapping(value = "/user/v1")
@RestController
public class UserController {

    private final UserService userService;

    /**
     * 직접 가입
     */
    @PostMapping("/users")
    public ResponseEntity<UserCreateDto.Response> create(@RequestBody UserCreateDto.Request request) {
        UserDto userDto = userService.create(UserDto.of(request.getUserId(), request.getPassword(), request.getNickname()));
        return ResponseEntity.ok(UserCreateDto.Response.from(userDto));
    }

    /**
     * 회원정보 조회(내 정보 조회)
     *
     * @return
     */
    @GetMapping("/users/me")
    public ResponseEntity<UserReadDto.Response> getMe(@RequestParam("id") String id) {
        UserDto userDto = userService.getUser(id);
        return ResponseEntity.ok(UserReadDto.Response.from(userDto));
    }

    /**
     * 회원정보 수정(내 정보 수정)
     *
     * @return
     */
    @PatchMapping("/users/me")
    public UserUpdateDto.Response update(UserUpdateDto.Request request) {
        return null;
    }

    /**
     * 비밀번호 수정(내 정보 수정)
     *
     * @return
     */
    @PatchMapping("/users/me/password")
    public UserUpdateDto.Response updatePassword(UserUpdateDto.PasswordRequest request) {
        return null;
    }

    /**
     * 회원정보 통합
     *
     * @return
     */
    @PostMapping("/users/me/merge-oauth")
    public UserMergeDto.Response merge(UserMergeDto.Request request) {
        return null;
    }

    /**
     * 회원탈퇴
     *
     * @return
     */
    @DeleteMapping("/users/me")
    public UserDeleteDto.Response delete() {
        return null;
    }
}
