package com.backend.userservice.user.event;

import java.util.UUID;

/**
 * 회원 탈퇴가 커밋된 뒤 interaction-service 등 연관 리소스 정리를 트리거하기 위한 도메인 이벤트.
 */
public record UserDeletedEvent(UUID userId) {
}
