package com.backend.commondataaccess.persistence.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Job 자체의 lifecycle (PENDING/RUNNING/...)
 */
public enum JobStatus {
    PENDING,    // 생성됐고 아직 실행 안 됨 (워커 픽업 대기)
    RUNNING,    // 워커가 잡고 실행 중
    SUCCESS,    // 정상 종료
    FAILED,     // 에러로 종료
    CANCELLED;   // 사용자가 취소

    @JsonValue
    public String getName() {
        return this.name().toLowerCase();
    }
}
