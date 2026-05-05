package com.backend.commondataaccess.persistence.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * PENDING/INDEXING/INDEXED/FAILED/SKIPPED
 */
@Getter
@AllArgsConstructor
public enum IndexingStatus {
    PENDING("Elasticsearch에 색인 처리 대기 상태"),
    INDEXING("Elasticsearch에 색인중"),
    INDEXED("Elasticsearch에 색인 완료"),
    FAILED("Elasticsearch에 색인 실패"),
    SKIPPED("동일한 contentHash 값이라 색인 스킵");

    private final String description;

    @JsonValue
    public String getName() {
        return this.name().toLowerCase();
    }
}
