package com.backend.userservice.user.bootstrap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.backend.userservice.user.service.UserService;
import com.backend.userservice.user.service.dto.UserDto;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

@DisplayName("AdminBootstrapRunner 테스트")
@ExtendWith(MockitoExtension.class)
class AdminBootstrapRunnerTest {

    @Mock
    private UserService userService;

    @Test
    void 활성_ADMIN이_있으면_생성하지_않는다() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties(true, "admin", "secret", "admin");
        AdminBootstrapRunner runner = new AdminBootstrapRunner(properties, userService);

        when(userService.hasAdmin()).thenReturn(true);

        runner.run(new DefaultApplicationArguments());

        verify(userService, never()).createAdmin(any());
    }

    @Test
    void 활성_ADMIN이_없으면_평문_비밀번호로_생성한다() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties(true, "admin", "plain-password", "운영자");
        AdminBootstrapRunner runner = new AdminBootstrapRunner(properties, userService);

        when(userService.hasAdmin()).thenReturn(false);
        when(userService.createAdmin(any(UserDto.class))).thenReturn(UserDto.builder().build());

        runner.run(new DefaultApplicationArguments());

        ArgumentCaptor<UserDto> captor = ArgumentCaptor.forClass(UserDto.class);
        verify(userService).createAdmin(captor.capture());
        UserDto created = captor.getValue();
        Assertions.assertThat(created.loginId()).isEqualTo("admin");
        Assertions.assertThat(created.password()).isEqualTo("plain-password");
        Assertions.assertThat(created.passwordConfirm()).isEqualTo("plain-password");
        Assertions.assertThat(created.nickname()).isEqualTo("운영자");
    }

    @Test
    void enabled인데_password가_비어있으면_기동을_실패시킨다() {
        AdminBootstrapProperties properties = new AdminBootstrapProperties(true, "admin", " ", "admin");
        AdminBootstrapRunner runner = new AdminBootstrapRunner(properties, userService);

        when(userService.hasAdmin()).thenReturn(false);

        Assertions.assertThatThrownBy(() -> runner.run(new DefaultApplicationArguments()))
                  .isInstanceOf(IllegalStateException.class)
                  .hasMessageContaining("ADMIN_PASSWORD");
    }
}
