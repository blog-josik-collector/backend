package com.backend.interactionservice.postbookmark.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.backend.interactionservice.post.service.dto.PostDocument;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * 즐겨찾기 목록 조회 시 post_id 에 해당하는 Elasticsearch 문서를 bulk 조회하는 repository.
 */
@Slf4j
@Repository
public class PostBookmarkElasticsearchRepository {

    private final String indexName;
    private final ElasticsearchClient elasticsearchClient;

    public PostBookmarkElasticsearchRepository(@Value("${elasticsearch.index-name}") String indexName,
                                               ElasticsearchClient elasticsearchClient) {

        this.indexName = indexName;
        this.elasticsearchClient = elasticsearchClient;
    }

    /**
     * 주어진 post_id 순서를 유지한 채 Elasticsearch 문서를 조회한다. ES 에 없는 id 는 결과에서 제외한다.
     */
    public List<PostDocument> findByIdsInOrder(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        try {
            List<String> idStrings = ids.stream()
                                        .map(UUID::toString)
                                        .toList();

            SearchRequest searchRequest = SearchRequest.of(s -> s.index(indexName)
                                                                 .query(q -> q.ids(i -> i.values(idStrings)))
                                                                 .size(ids.size()));

            SearchResponse<PostDocument> response = elasticsearchClient.search(searchRequest, PostDocument.class);

            Map<UUID, PostDocument> documentById = new HashMap<>();
            for (Hit<PostDocument> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    documentById.put(hit.source().id(), hit.source());
                }
            }

            List<PostDocument> ordered = new ArrayList<>();
            for (UUID id : ids) {
                PostDocument document = documentById.get(id);
                if (document != null) {
                    ordered.add(document);
                }
            }
            return ordered;

        } catch (IOException e) {
            log.error("[ES Bookmark Search Error] Elasticsearch 통신 장애 발생", e);
            throw new IllegalStateException("검색 시스템 내부 장애가 발생했습니다.", e);
        }
    }
}
