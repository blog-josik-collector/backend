package com.backend.commonelasticsearch.operation;

/**
 * Elasticsearch client API 종류. {@link com.backend.commonelasticsearch.client.ApplicationElasticsearchClient} 로그/변환 컨텍스트에 사용한다.
 */
public enum ElasticsearchOperation {
    GET,
    SEARCH,
    BULK,
    EXISTS,
    GET_ALIAS,
    CREATE_INDEX,
    UPDATE_ALIASES,
    REINDEX
}
