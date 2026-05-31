package com.backend.interactionservice.post.repository;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.GetResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.interactionservice.post.repository.query.SearchCondition;
import com.backend.interactionservice.post.service.dto.PostDocument;
import java.io.IOException;
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
 * techblog-posts-v1 인덱스에서 포스팅 문서를 조회하는 Elasticsearch repository. <br>
 * 단건 get, 검색(search) API 호출과 ES 예외 변환을 담당한다.
 */
@Slf4j
@Repository
public class PostElasticsearchRepository {

    private final String indexName;
    private final ElasticsearchClient elasticsearchClient;

    public PostElasticsearchRepository(@Value("${elasticsearch.index-name}") String indexName,
                                       ElasticsearchClient elasticsearchClient) {

        this.indexName = indexName;
        this.elasticsearchClient = elasticsearchClient;
    }

    public Optional<PostDocument> findById(UUID id) {
        try {
            GetResponse<PostDocument> response = elasticsearchClient.get(g -> g.index(indexName)
                                                                               .id(id.toString()),
                                                                           PostDocument.class);

            if (response.found() && response.source() != null) {
                return Optional.of(response.source());
            }

            log.warn("Elasticsearch 문서가 존재하지 않습니다. _id: {}", id);
            return Optional.empty();

        } catch (IOException e) {
            log.error("[ES Get Error] Elasticsearch 통신 장애 발생", e);
            throw new IllegalStateException("검색 시스템 내부 장애가 발생했습니다.", e);
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            log.error("Elasticsearch 단건 조회 중 장애 발생: {}", e.getMessage(), e);
            throw new IllegalStateException("검색 엔진 내부 장애가 발생했습니다.", e);
        }
    }

    public OffsetPageResult<PostDocument> search(SearchCondition condition, Pageable pageable) {
        try {
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

            SearchRequest searchRequest = SearchRequest.of(s -> s.index(indexName)
                                                                 .query(new Query(boolQueryBuilder.build()))
                                                                 .from(from)
                                                                 .size(size)
                                                                 .sort(so -> so.field(f -> f.field("likeCount")
                                                                                            .order(SortOrder.Desc))));

            log.info("[ES Search] Request Query From: {}, Size: {}", from, size);

            SearchResponse<PostDocument> response = elasticsearchClient.search(searchRequest, PostDocument.class);

            List<PostDocument> contents = new ArrayList<>();
            for (Hit<PostDocument> hit : response.hits().hits()) {
                if (hit.source() != null) {
                    contents.add(hit.source());
                }
            }

            long totalHits = response.hits().total() != null ? response.hits().total().value() : 0;
            return new OffsetPageResult<>(totalHits, from, size, contents);

        } catch (IOException e) {
            log.error("[ES Search Error] Elasticsearch 통신 장애 발생", e);
            throw new IllegalStateException("검색 시스템 내부 장애가 발생했습니다.", e);
        }
    }
}
