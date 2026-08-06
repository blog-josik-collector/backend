package com.backend.interactionservice.postbookmark.repository;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.backend.commonelasticsearch.client.ApplicationElasticsearchClient;
import com.backend.commonelasticsearch.operation.ElasticsearchOperation;
import com.backend.interactionservice.post.service.dto.PostDocument;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * 즐겨찾기 목록 조회 시 post_id 에 해당하는 Elasticsearch 문서를 bulk 조회하는 repository.
 */
@Repository
public class PostBookmarkElasticsearchRepository {

    private final String indexAlias;
    private final ApplicationElasticsearchClient applicationElasticsearchClient;

    public PostBookmarkElasticsearchRepository(@Value("${elasticsearch.index-alias}") String indexAlias,
                                               ApplicationElasticsearchClient applicationElasticsearchClient) {

        this.indexAlias = indexAlias;
        this.applicationElasticsearchClient = applicationElasticsearchClient;
    }

    /**
     * 주어진 post_id 순서를 유지한 채 Elasticsearch 문서를 조회한다. ES 에 없는 id 는 결과에서 제외한다.
     */
    public List<PostDocument> findByIdsInOrder(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }

        List<String> idStrings = ids.stream()
                                    .map(UUID::toString)
                                    .toList();

        SearchRequest searchRequest = SearchRequest.of(s -> s.index(indexAlias)
                                                             .query(q -> q.ids(i -> i.values(idStrings)))
                                                             .size(ids.size()));

        SearchResponse<PostDocument> response = applicationElasticsearchClient.execute(
                ElasticsearchOperation.SEARCH,
                client -> client.search(searchRequest, PostDocument.class));

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
    }
}
