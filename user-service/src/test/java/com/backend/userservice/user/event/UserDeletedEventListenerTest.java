package com.backend.userservice.user.event;

import com.backend.userservice.client.InteractionServiceClient;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("UserDeletedEventListener 테스트")
@ExtendWith(MockitoExtension.class)
class UserDeletedEventListenerTest {

    @InjectMocks
    private UserDeletedEventListener userDeletedEventListener;

    @Mock
    private InteractionServiceClient interactionServiceClient;

    @Test
    void 회원_탈퇴_이벤트를_받으면_interaction_삭제를_요청한다() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        userDeletedEventListener.onUserDeleted(new UserDeletedEvent(userId));

        // then
        Mockito.verify(interactionServiceClient).softDeleteUserInteractions(userId);
    }

    @Test
    void interaction_삭제_요청이_실패해도_예외를_전파하지_않는다() {
        // given
        UUID userId = UUID.randomUUID();

        Mockito.doThrow(new RuntimeException("test_error"))
               .when(interactionServiceClient)
               .softDeleteUserInteractions(userId);

        // when & then
        Assertions.assertThatCode(() -> userDeletedEventListener.onUserDeleted(new UserDeletedEvent(userId)))
                  .doesNotThrowAnyException();
        Mockito.verify(interactionServiceClient).softDeleteUserInteractions(userId);
    }
}
