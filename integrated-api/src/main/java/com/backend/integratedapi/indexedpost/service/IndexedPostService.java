package com.backend.integratedapi.indexedpost.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.GetResponse;
import com.backend.integratedapi.indexedpost.service.dto.IndexedPost;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class IndexedPostService {

    private final ElasticsearchClient esClient;
    private final String indexName;

    public IndexedPostService(ElasticsearchClient esClient,
                              @Value("${elasticsearch.index-name}") String indexName) {

        this.esClient = esClient;
        this.indexName = indexName;
    }

    public Optional<IndexedPost> getPost(UUID id) throws IOException {
        try {
            // 1. ES 단건 Get API 호출
            GetResponse<IndexedPost> response = esClient.get(g -> g
                                                                     .index(indexName)
                                                                     .id(id.toString()),
                                                             IndexedPost.class
            );

            // 2. 문서 존재 여부 확인 후 결과 반환
            if (response.found() && response.source() != null) {
                return Optional.of(response.source());
            }

            log.warn("Elasticsearch 문서가 존재하지 않습니다. _id: {}", id);
            return Optional.empty();

        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            // 인덱스가 존재하지 않거나 클러스터 통신 장애 시 예외 격리
            log.error("Elasticsearch 단건 조회 중 장애 발생: {}", e.getMessage(), e);
            throw new RuntimeException("검색 엔진 내부 장애가 발생했습니다.", e);
        }
    }
}
