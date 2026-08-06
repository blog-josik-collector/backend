package com.backend.integratedapi.indexedpost.repository;

import co.elastic.clients.elasticsearch.core.GetResponse;
import com.backend.commonelasticsearch.client.ApplicationElasticsearchClient;
import com.backend.commonelasticsearch.operation.ElasticsearchOperation;
import com.backend.integratedapi.indexedpost.service.dto.IndexedPost;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

/**
 * techblog-posts 인덱스에서 포스팅 문서를 조회하는 Elasticsearch repository.
 */
@Slf4j
@Repository
public class IndexedPostElasticsearchRepository {

    private final String indexAlias;
    private final ApplicationElasticsearchClient applicationElasticsearchClient;

    public IndexedPostElasticsearchRepository(@Value("${elasticsearch.index-alias}") String indexAlias,
                                              ApplicationElasticsearchClient applicationElasticsearchClient) {

        this.indexAlias = indexAlias;
        this.applicationElasticsearchClient = applicationElasticsearchClient;
    }

    public Optional<IndexedPost> fetchOneById(UUID id) {
        GetResponse<IndexedPost> response = applicationElasticsearchClient.execute(
                ElasticsearchOperation.GET,
                client -> client.get(g -> g.index(indexAlias).id(id.toString()), IndexedPost.class));

        if (response.found() && response.source() != null) {
            return Optional.of(response.source());
        }

        log.debug("[Post] ES document not found postId={}", id);
        return Optional.empty();
    }
}
