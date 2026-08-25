package com.backend.userservice.user.event;

import com.backend.userservice.client.InteractionServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 회원 탈퇴 커밋 후 interaction 데이터를 soft-delete 한다.
 * <p>
 * 탈퇴 트랜잭션은 이미 커밋된 상태이므로, 실패해도 회원 soft-delete 는 롤백되지 않는다.
 * 실패 시 로그를 남기고, 필요하면 재시도/보상 처리를 추가한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserDeletedEventListener {

    private final InteractionServiceClient interactionServiceClient;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserDeleted(UserDeletedEvent event) {
        try {
            interactionServiceClient.softDeleteUserInteractions(event.userId());
            log.info("[UserDeleted] interaction soft-delete requested userId={}", event.userId());
        } catch (Exception e) {
            log.error("[UserDeleted] interaction soft-delete failed userId={}", event.userId(), e);
        }
    }
}
