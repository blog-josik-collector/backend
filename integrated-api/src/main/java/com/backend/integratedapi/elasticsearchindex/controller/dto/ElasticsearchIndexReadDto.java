package com.backend.integratedapi.elasticsearchindex.controller.dto;

import com.backend.commonelasticsearch.provision.ProvisionResult;
import com.backend.commonelasticsearch.provision.ReindexResult;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Elasticsearch 인덱스 관리 API 응답 DTO 모음.
 */
public record ElasticsearchIndexReadDto() {

    /**
     * alias 현재 상태 조회 응답.
     */
    @Schema(description = "Elasticsearch alias 상태 조회 결과")
    public record Status(
            @Schema(description = "alias 이름", example = "techblog-posts")
            String alias,

            @Schema(description = "alias가 가리키는 현재 인덱스 이름", example = "techblog-posts-v1")
            String currentIndex,

            @Schema(description = "alias 존재 여부")
            boolean exists) {

    }

    /**
     * alias 부트스트랩(없으면 생성) 응답.
     */
    @Schema(description = "Elasticsearch alias 부트스트랩 결과")
    public record Bootstrap(
            @Schema(description = "alias 이름")
            String alias,

            @Schema(description = "생성된 또는 기존 인덱스 이름")
            String index,

            @Schema(description = "이번 호출에서 인덱스를 새로 생성했는지 여부")
            boolean created) {

        public static Bootstrap from(ProvisionResult result) {
            return new Bootstrap(result.alias(), result.index(), result.created());
        }
    }

    /**
     * 재색인 + alias 스왑 응답.
     */
    @Schema(description = "Elasticsearch 재색인 결과")
    public record Reindex(
            @Schema(description = "alias 이름")
            String alias,

            @Schema(description = "재색인 원본 인덱스 이름")
            String sourceIndex,

            @Schema(description = "재색인 후 alias가 가리키는 새 인덱스 이름")
            String newIndex,

            @Schema(description = "재색인된 문서 수")
            long documents) {

        public static Reindex from(ReindexResult result) {
            return new Reindex(result.alias(), result.sourceIndex(), result.newIndex(), result.documents());
        }
    }
}
