package com.backend.integratedapi.elasticsearchindex.controller.dto;

import com.backend.commonelasticsearch.provision.ProvisionResult;
import com.backend.commonelasticsearch.provision.ReindexResult;

/**
 * Elasticsearch 인덱스 관리 API 응답 DTO 모음.
 */
public record ElasticsearchIndexReadDto() {

    /**
     * alias 현재 상태 조회 응답.
     */
    public record Status(String alias, String currentIndex, boolean exists) {

    }

    /**
     * alias 부트스트랩(없으면 생성) 응답.
     */
    public record Bootstrap(String alias, String index, boolean created) {

        public static Bootstrap from(ProvisionResult result) {
            return new Bootstrap(result.alias(), result.index(), result.created());
        }
    }

    /**
     * 재색인 + alias 스왑 응답.
     */
    public record Reindex(String alias, String sourceIndex, String newIndex, long documents) {

        public static Reindex from(ReindexResult result) {
            return new Reindex(result.alias(), result.sourceIndex(), result.newIndex(), result.documents());
        }
    }
}
