package com.backend.commondataaccess.persistence.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IndexingStatus {
    INDEX_PENDING("Elasticsearch에 색인 처리 대기 상태"),
    INDEXED("Elasticsearch에 색인 완료"),
    FAILED_INDEX("Elasticsearch에 색인 실패");

    private final String description;
}
