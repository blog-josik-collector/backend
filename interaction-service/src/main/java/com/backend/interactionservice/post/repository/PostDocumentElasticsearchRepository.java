package com.backend.interactionservice.post.repository;

import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commonelasticsearch.client.ApplicationElasticsearchClient;
import com.backend.commonelasticsearch.operation.ElasticsearchOperation;
import com.backend.interactionservice.post.repository.query.SearchCondition;
import com.backend.interactionservice.post.service.dto.PostDocument;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

/**
 * techblog-posts 인덱스에서 포스팅 문서를 조회하는 Elasticsearch repository.
 */
@Slf4j
@Repository
public class PostDocumentElasticsearchRepository {

    private final String indexAlias;
    private final ApplicationElasticsearchClient applicationElasticsearchClient;

    public PostDocumentElasticsearchRepository(@Value("${elasticsearch.index-alias}") String indexAlias,
                                               ApplicationElasticsearchClient applicationElasticsearchClient) {

        this.indexAlias = indexAlias;
        this.applicationElasticsearchClient = applicationElasticsearchClient;
    }

    public Optional<PostDocument> fetchOneById(UUID id) {
        GetResponse<PostDocument> response = applicationElasticsearchClient.execute(
                ElasticsearchOperation.GET,
                client -> client.get(g -> g.index(indexAlias).id(id.toString()), PostDocument.class));

        if (response.found() && response.source() != null) {
            return Optional.of(response.source());
        }

        log.debug("[Post] ES document not found postId={}", id);
        return Optional.empty();
    }

    public OffsetPageResult<PostDocument> search(SearchCondition condition, Pageable pageable) {
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

        boolQueryBuilder.filter(f -> f.term(t -> t.field("status").value(PostStatus.ACTIVE.getName())));

        if (StringUtils.hasText(condition.getProvider())) {
            boolQueryBuilder.filter(f -> f.term(t -> t.field("provider").value(condition.getProvider().toLowerCase())));
        }

        if (StringUtils.hasText(condition.getTitle())) {
            boolQueryBuilder.must(m -> m.match(mt -> mt.field("title").query(condition.getTitle())));
        } else {
            boolQueryBuilder.must(m -> m.matchAll(ma -> ma));
        }

        int from = (int) pageable.getOffset();
        int size = pageable.getPageSize();

        SearchRequest searchRequest = SearchRequest.of(s -> s.index(indexAlias)
                                                             .query(new Query(boolQueryBuilder.build()))
                                                             .from(from)
                                                             .size(size)
                                                             .sort(so -> so.field(f -> f.field("publishedAt")
                                                                                        .order(SortOrder.Desc))));

        SearchResponse<PostDocument> response = applicationElasticsearchClient.execute(
                ElasticsearchOperation.SEARCH,
                client -> client.search(searchRequest, PostDocument.class));

        List<PostDocument> contents = new ArrayList<>();
        for (Hit<PostDocument> hit : response.hits().hits()) {
            if (hit.source() != null) {
                contents.add(hit.source());
            }
        }

        long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
        return new OffsetPageResult<>(totalHits, from, size, contents);
    }
}
