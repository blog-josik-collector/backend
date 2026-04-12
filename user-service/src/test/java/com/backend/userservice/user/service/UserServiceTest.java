package com.backend.userservice.user.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.UserAuthentication;
import com.backend.commondataaccess.persistence.user.enums.LoginProvider;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.userservice.user.repository.UserQueryRepository;
import com.backend.userservice.user.repository.UserRepository;
import com.backend.userservice.user.service.dto.UserDto;
import com.backend.userservice.userauthentication.service.UserAuthenticationService;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("UserService 테스트")
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Spy
    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserQueryRepository queryRepository;

    @Mock
    private UserAuthenticationService userAuthenticationService;

    private User mockUser;

    @BeforeEach
    void init() {
        mockUser = User.builder()
                       .id(UUID.randomUUID())
                       .userType(UserType.USER)
                       .nickname("test_nickname")
                       .build();
    }

    @DisplayName("User 생성 테스트")
    @Nested
    class CreateUserTest {

        @Test
        void user_dto를_통해_User_를_생성할_수_있다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";
            String passwordConfirm = "test_password";

            UserDto userDto = UserDto.builder()
                                     .loginId(loginId)
                                     .nickname(mockUser.nickname())
                                     .password(password)
                                     .passwordConfirm(passwordConfirm)
                                     .build();

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.LOCAL)
                                                                      .identifier(loginId)
                                                                      .credential(password)
                                                                      .build();

            Mockito.doReturn(Boolean.FALSE).when(userRepository).existsByNickname(any());
            Mockito.doReturn(mockUser).when(userRepository).save(any());

            Mockito.doReturn(userAuthentication).when(userAuthenticationService).create(any(), any(), any(), any());

            // when
            UserDto createdUser = userService.create(userDto);

            // then
            Assertions.assertThat(createdUser).isNotNull();
            Assertions.assertThat(createdUser.userId()).isEqualTo(mockUser.id());
            Assertions.assertThat(createdUser.nickname()).isEqualTo(mockUser.nickname());
            Assertions.assertThat(createdUser.userType()).isEqualTo(mockUser.userType());
        }

        @Test
        void subject를_통해_User_를_생성할_수_있다() {
            // given
            String subject = "test_subject";

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.GOOGLE)
                                                                      .identifier(subject)
                                                                      .build();

            Mockito.doReturn(Boolean.FALSE).when(userRepository).existsByNickname(any());
            Mockito.doReturn(mockUser).when(userRepository).save(any());

            Mockito.doReturn(userAuthentication).when(userAuthenticationService).create(any(), any());

            // when
            User createdUser = userService.create(subject);

            // then
            Assertions.assertThat(createdUser).isNotNull();
            Assertions.assertThat(createdUser.id()).isEqualTo(mockUser.id());
            Assertions.assertThat(createdUser.nickname()).isNotNull();
            Assertions.assertThat(createdUser.userType()).isEqualTo(mockUser.userType());
        }
    }

    @DisplayName("User 조회 테스트")
    @Nested
    class ReadUserTest {

        @Test
        void id를_입력하면_User를_조회할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockUser)).when(queryRepository).findOneById(any());

            // when
            User user = userService.getUser(mockUser.id());

            // then
            Assertions.assertThat(user).isNotNull();
            Assertions.assertThat(user.id()).isEqualTo(mockUser.id());
            Assertions.assertThat(user.nickname()).isEqualTo(mockUser.nickname());
            Assertions.assertThat(user.userType()).isEqualTo(mockUser.userType());
        }

        @Test
        void id를_입력하면_UserDto를_조회할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockUser)).when(queryRepository).findOneById(any());

            // when
            UserDto userDto = userService.getUserDto(mockUser.id());

            // then
            Assertions.assertThat(userDto).isNotNull();
            Assertions.assertThat(userDto.userId()).isEqualTo(mockUser.id());
            Assertions.assertThat(userDto.nickname()).isEqualTo(mockUser.nickname());
            Assertions.assertThat(userDto.userType()).isEqualTo(mockUser.userType());
        }

        @Test
        void UserDto를_입력하면_UserDto를_조회할_수_있다() {
            // given
            UserDto userDto = UserDto.of(mockUser.id(), mockUser.nickname());

            Mockito.doReturn(Optional.of(mockUser)).when(queryRepository).findOneById(any());

            // when
            UserDto result = userService.getUserDto(userDto);

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result.userId()).isEqualTo(mockUser.id());
            Assertions.assertThat(result.nickname()).isEqualTo(mockUser.nickname());
        }
    }

    @DisplayName("User 수정 테스트")
    @Nested
    class UpdateUserTest {

        @Test
        void user_nickname을_업데이트_할_수_있다() {
            // given
            String nickName = "test_updated_nickname";

            UserDto updateRequest = UserDto.builder()
                                           .userId(mockUser.id())
                                           .nickname(nickName)
                                           .build();

            Mockito.doReturn(Boolean.FALSE).when(userRepository).existsByNickname(any());
            Mockito.doReturn(Optional.of(mockUser)).when(queryRepository).findOneById(any());

            Assertions.assertThat(mockUser.nickname()).isNotEqualTo(nickName);

            // when
            userService.update(updateRequest);

            // then
            Assertions.assertThat(mockUser.nickname()).isEqualTo(nickName);
        }

        @Test
        void user_password를_업데이트_할_수_있다() {
            // given
            String password = "test_password";
            String newPassword = "test_new_password";

            // userAuthenticationService.updatePassword 메소드에 대한 검증은 UserAuthenticationServiceTest 클래스에서 완료
            Mockito.doNothing().when(userAuthenticationService).updatePassword(any(), any(), any());

            // when
            userService.updatePassword(mockUser.id(), password, newPassword);
        }

        @Test
        void user를_merge_할_수_있다() {
            // given
            String loginId = "test_login_id";
            String password = "test_password";

            User mockUser2 = User.builder()
                                 .id(UUID.randomUUID())
                                 .userType(UserType.USER)
                                 .nickname("test_nickname2")
                                 .build();

            UserAuthentication userAuthentication = UserAuthentication.builder()
                                                                      .user(mockUser)
                                                                      .loginProvider(LoginProvider.LOCAL)
                                                                      .identifier(loginId)
                                                                      .credential(password)
                                                                      .build();

            // userAuthenticationService.merge 메소드에 대한 검증은 UserAuthenticationServiceTest 클래스에서 완료
            Mockito.doNothing().when(userAuthenticationService).merge(any(), any());
            Mockito.doReturn(Optional.of(mockUser)).when(queryRepository).findOneById(any());

            // when
            userService.merge(userAuthentication.id(), mockUser2.id());
        }
    }

    @DisplayName("User 삭제 테스트")
    @Nested
    class DeleteUserTest {

        @Test
        void user를_삭제_할_수_있다() {
            // given
            // userAuthenticationService.merge 메소드에 대한 검증은 UserAuthenticationServiceTest 클래스에서 완료
            Mockito.doNothing().when(userAuthenticationService).deleteAll(any());
            Mockito.doReturn(Optional.of(mockUser)).when(queryRepository).findOneById(any());

            // when
            userService.delete(mockUser.id());

            // then
            Assertions.assertThat(mockUser.deletedAt()).isNotNull();
            Assertions.assertThat(mockUser.isDelete()).isEqualTo(true);
        }
    }

}