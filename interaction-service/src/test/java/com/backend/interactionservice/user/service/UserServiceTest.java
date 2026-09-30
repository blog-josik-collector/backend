package com.backend.interactionservice.user.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.interactionservice.user.repository.UserQueryRepository;
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
    private UserQueryRepository userQueryRepository;

    private User mockUser;

    @BeforeEach
    void init() {
        mockUser = User.builder()
                       .id(UUID.randomUUID())
                       .userType(UserType.USER)
                       .nickname("test_nickname")
                       .build();
    }

    @DisplayName("User 조회 테스트")
    @Nested
    class GetUserTest {

        @Test
        void id를_입력하면_User를_조회할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockUser)).when(userQueryRepository).fetchOneById(any());

            // when
            User result = userService.getUser(mockUser.id());

            // then
            Assertions.assertThat(result).isSameAs(mockUser);
        }

        @Test
        void id가_null이면_User_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> userService.getUser(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }

        @Test
        void 존재하지_않는_id로_조회하면_User_조회에_실패한다() {
            // given
            Mockito.doReturn(Optional.empty()).when(userQueryRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> userService.getUser(UUID.randomUUID()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 user입니다.");
        }
    }
}
