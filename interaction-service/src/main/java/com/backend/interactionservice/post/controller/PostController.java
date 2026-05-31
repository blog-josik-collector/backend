package com.backend.interactionservice.post.controller;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.interactionservice.post.repository.query.SearchCondition;
import com.backend.interactionservice.post.service.PostService;
import com.backend.interactionservice.post.service.dto.PostListItem;
import com.backend.interactionservice.post.service.PostViewCountService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "01. 포스팅 조회 API")
@RequestMapping("/interaction/v1")
@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postSearchService;
    private final PostViewCountService postViewCountService;

    /**
     * 기술 블로그 포스팅 키워드 검색 및 필터링, 정렬 페이징 API
     * <p>
     * 응답에는 현재 사용자의 좋아요/북마크 여부(likesOfMe, bookmarksOfMe)가 포함된다.
     * 인증되지 않은 호출의 경우 두 값은 항상 false 이다.
     */
    @GetMapping("/postings")
    public ResponseEntity<OffsetPageResult<PostListItem>> searchPostings(@RequestParam(required = false) String title,
                                                                         @RequestParam(required = false) String provider,
                                                                         @PageableDefault(size = 20) Pageable pageable,
                                                                         @AuthenticationPrincipal JwtPrincipal principal) {

        SearchCondition searchCondition = SearchCondition.builder()
                                                   .title(title)
                                                   .provider(provider)
                                                   .build();

        UUID userId = principal != null ? principal.getUserId() : null;

        OffsetPageResult<PostListItem> result = postSearchService.searchPosts(searchCondition, userId, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * 기술 블로그 포스팅 단건 조회 API
     * <p>
     * 응답에는 현재 사용자의 좋아요/북마크 여부(likesOfMe, bookmarksOfMe)가 포함된다.
     * 인증되지 않은 호출의 경우 두 값은 항상 false 이며, 해당 포스팅이 존재하지 않으면 404를 반환한다.
     * <p>
     * 정상 조회된 경우(found == true)에 한해 view 카운트를 1 증가시킨다 (Redis 누적, DB 반영은 PostViewCountFlushWorker 가 일괄 처리).
     */
    @GetMapping("/postings/{postId}")
    public ResponseEntity<PostListItem> searchPost(@PathVariable UUID postId,
                                                   @AuthenticationPrincipal JwtPrincipal principal) {

        UUID userId = principal != null ? principal.getUserId() : null;

        Optional<PostListItem> result = postSearchService.searchPost(postId, userId);
        result.ifPresent(item -> postViewCountService.recordView(postId));

        return result.map(ResponseEntity::ok)
                     .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
