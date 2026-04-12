package com.backend.userservice.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.commondataaccess.security.MockJwtPrincipalResolver;
import com.backend.userservice.user.controller.dto.UserCreateDto;
import com.backend.userservice.user.controller.dto.UserCreateDto.Request;
import com.backend.userservice.user.controller.dto.UserMergeDto;
import com.backend.userservice.user.controller.dto.UserUpdateDto;
import com.backend.userservice.user.service.UserService;
import com.backend.userservice.user.service.dto.UserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.UUID;
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

@DisplayName("UserController 테스트")
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private UserController userController;

    @Mock
    private UserService userService;

    private User mockUser;

    @BeforeEach
    void init() {
        mockUser = User.builder()
                       .id(UUID.randomUUID())
                       .userType(UserType.USER)
                       .nickname("test_nickname")
                       .build();

        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                                 .setCustomArgumentResolvers(new MockJwtPrincipalResolver())
                                 .build();
    }

    private UserDto createResponseDto(User user) {
        return UserDto.builder()
                      .userId(user.id())
                      .nickname(user.nickname())
                      .userType(user.userType())
                      .createdAt(user.createdAt())
                      .updatedAt(user.updatedAt())
                      .lastLoginAt(user.lastLoginAt())
                      .build();
    }

    @Test
    void 직접_회원가입을_한다() throws Exception {
        String loginId = "test_login_id";
        String password = "test_password";
        String base64Password = Base64.getEncoder().encodeToString(password.getBytes());
        String passwordConfirm = "test_password";
        String base64PasswordConfirm = Base64.getEncoder().encodeToString(passwordConfirm.getBytes());
        String nickname = "test_nickname";

        UserCreateDto.Request request = new Request(loginId, base64Password, base64PasswordConfirm, nickname);

        Mockito.doReturn(createResponseDto(mockUser))
               .when(userService).create(any(UserDto.class));

        mockMvc.perform(post("/user/v1/users")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.userId").value(mockUser.id().toString()))
               .andExpect(jsonPath("$.createdAt").value(mockUser.createdAt()));
    }

    @Test
    void 회원정보를_조회_한다() throws Exception {
        Mockito.doReturn(mockUser).when(userService).getUser(any());

        mockMvc.perform(get("/user/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.userId").value(mockUser.id().toString()))
               .andExpect(jsonPath("$.userType").value(mockUser.userType().toString()))
               .andExpect(jsonPath("$.nickname").value(mockUser.nickname()))
               .andExpect(jsonPath("$.createdAt").value(mockUser.createdAt()))
               .andExpect(jsonPath("$.updatedAt").value(mockUser.updatedAt()))
               .andExpect(jsonPath("$.lastLoginAt").value(mockUser.lastLoginAt()));
    }

    @Test
    void 회원정보를_수정_한다() throws Exception {
        String nickname = "test_nickname";
        UserUpdateDto.Request request = new UserUpdateDto.Request(nickname);

        Mockito.doNothing().when(userService).update(any(UserDto.class));
        Mockito.doReturn(UserDto.of(mockUser.id(), nickname)).when(userService).getUserDto(any(UserDto.class));

        mockMvc.perform(patch("/user/v1/users/me")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.userId").value(mockUser.id().toString()))
               .andExpect(jsonPath("$.updatedAt").value(mockUser.updatedAt()));
    }

    @Test
    void 비밀번호를_수정_한다() throws Exception {
        String password = "test_password";
        String base64Password = Base64.getEncoder().encodeToString(password.getBytes());
        String newPassword = "test_new_password";
        String base64NewPassword = Base64.getEncoder().encodeToString(newPassword.getBytes());
        UserUpdateDto.PasswordRequest request = new UserUpdateDto.PasswordRequest(base64Password, base64NewPassword);

        Mockito.doNothing().when(userService).updatePassword(any(), any(), any());
        Mockito.doReturn(createResponseDto(mockUser)).when(userService).getUserDto(any(UUID.class));

        mockMvc.perform(patch("/user/v1/users/me/password")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.userId").value(mockUser.id().toString()))
               .andExpect(jsonPath("$.updatedAt").value(mockUser.updatedAt()));
    }

    @Test
    void 회원정보를_통합_한다() throws Exception {
        UserMergeDto.Request request = new UserMergeDto.Request(UUID.randomUUID());

        Mockito.doNothing().when(userService).merge(any(), any());

        mockMvc.perform(post("/user/v1/users/me/merge-oauth")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted());
    }

    @Test
    void 회원탈퇴를_한다() throws Exception {
        Mockito.doNothing().when(userService).delete(any(UUID.class));

        mockMvc.perform(delete("/user/v1/users/me")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted());
    }
}