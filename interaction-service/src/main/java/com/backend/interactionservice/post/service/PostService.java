package com.backend.interactionservice.post.service;

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
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.repository.query.SearchCondition;
import com.backend.interactionservice.post.service.dto.PostDocument;
import com.backend.interactionservice.post.service.dto.PostListItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class PostService {

    private final ElasticsearchClient elasticsearchClient;
    private final PostQueryRepository postQueryRepository;
    private final String indexName;

    public PostService(ElasticsearchClient elasticsearchClient,
                       PostQueryRepository postQueryRepository,
                       @Value("${elasticsearch.index-name}") String indexName) {

        this.elasticsearchClient = elasticsearchClient;
        this.postQueryRepository = postQueryRepository;
        this.indexName = indexName;
    }

    public Post getPost(UUID id) {
        Optional<Post> post = postQueryRepository.fetchOneById(id);
        return post.get();
    }

    public OffsetPageResult<PostListItem> searchPosts(SearchCondition condition, UUID userId, Pageable pageable) {
        OffsetPageResult<PostDocument> esResult = searchFromElasticsearch(condition, pageable);

        List<PostDocument> documents = esResult.getItems();
        if (documents.isEmpty()) {
            return new OffsetPageResult<>(esResult.getTotalCount(), esResult.getPage(), esResult.getSize(), List.of());
        }

        List<UUID> postIds = documents.stream()
                                      .map(PostDocument::id)
                                      .toList();

        Set<UUID> likedIds = postQueryRepository.findLikedPostIds(userId, postIds);
        Set<UUID> bookmarkedIds = postQueryRepository.findBookmarkedPostIds(userId, postIds);

        return esResult.map(doc -> PostListItem.of(doc,
                                                   likedIds.contains(doc.id()),
                                                   bookmarkedIds.contains(doc.id())));
    }

    public Optional<PostListItem> searchPost(UUID postId, UUID userId) {
        return searchPost(postId).map(doc -> {
            List<UUID> postIds = List.of(doc.id());
            boolean likesOfMe = !postQueryRepository.findLikedPostIds(userId, postIds).isEmpty();
            boolean bookmarksOfMe = !postQueryRepository.findBookmarkedPostIds(userId, postIds).isEmpty();
            return PostListItem.of(doc, likesOfMe, bookmarksOfMe);
        });
    }

    private Optional<PostDocument> searchPost(UUID id) {
        try {
            // 1. ES 단건 Get API 호출
            GetResponse<PostDocument> response = elasticsearchClient.get(g -> g
                                                                                 .index(indexName)
                                                                                 .id(id.toString()),
                                                                         PostDocument.class
            );

            // 2. 문서 존재 여부 확인 후 결과 반환
            if (response.found() && response.source() != null) {
                return Optional.of(response.source());
            }

            log.warn("Elasticsearch 문서가 존재하지 않습니다. _id: {}", id);
            return Optional.empty();

        } catch (IOException e) {
            log.error("[ES Get Error] Elasticsearch 통신 장애 발생", e);
            throw new RuntimeException("검색 시스템 내부 장애가 발생했습니다.", e);
        } catch (co.elastic.clients.elasticsearch._types.ElasticsearchException e) {
            log.error("Elasticsearch 단건 조회 중 장애 발생: {}", e.getMessage(), e);
            throw new RuntimeException("검색 엔진 내부 장애가 발생했습니다.", e);
        }
    }

    private OffsetPageResult<PostDocument> searchFromElasticsearch(SearchCondition condition, Pageable pageable) {
        try {
            // 1. Bool Query 빌더 생성
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            // 1-1. 무조건 대기/노출 상태가 ACTIVE인 것만 필터링 (Filter Cache 활용)
            boolQueryBuilder.filter(f -> f.term(t -> t.field("status").value(PostStatus.ACTIVE.getName())));

            // 1-2. 조건부 필터: Provider가 제공되었을 때 (Filter Cache 활용)
            if (StringUtils.hasText(condition.getProvider())) {
                boolQueryBuilder.filter(f -> f.term(t -> t.field("provider").value(condition.getProvider().toLowerCase())));
            }

            // 1-3. 조건부 검색: 검색어(Keyword)가 제목(title)에 매칭될 때 (Inverted Index 매칭 및 스코어링)
            if (StringUtils.hasText(condition.getTitle())) {
                boolQueryBuilder.must(m -> m.match(mt -> mt.field("title").query(condition.getTitle())));
            } else {
                // 검색어가 없으면 전체 매칭 (MatchAll) 구조 처리
                boolQueryBuilder.must(m -> m.matchAll(ma -> ma));
            }

            // 2. 최종 SearchRequest 빌드 (페이징 + 정렬 최적화)
            int from = (int) pageable.getOffset();
            int size = pageable.getPageSize();

            SearchRequest searchRequest = SearchRequest.of(s -> s
                    .index(indexName)
                    .query(new Query(boolQueryBuilder.build()))
                    .from(from) // 페이징시작 위치 (Offset)
                    .size(size) // 가져올 개수 (Limit)
                    .sort(so -> so.field(f -> f.field("like_count").order(SortOrder.Desc))) // Doc Values 정렬 적용
            );

            log.info("[ES Search] Request Query From: {}, Size: {}", from, size);

            // 3. Elasticsearch 클라이언트 실행
            SearchResponse<PostDocument> response = elasticsearchClient.search(searchRequest, PostDocument.class);

            // 4. 결과를 OffsetPageResult로 변환 (ES 응답 순서 보존)
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
            throw new RuntimeException("검색 시스템 내부 장애가 발생했습니다.", e);
        }
    }
}
