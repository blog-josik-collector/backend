package com.backend.interactionservice.postbookmark.controller;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.security.CurrentUser;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.interactionservice.post.service.dto.PostDocument;
import com.backend.interactionservice.postbookmark.service.PostBookmarkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "03. 포스팅 즐겨찾기 API")
@RequestMapping(value = "/interaction/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequiredArgsConstructor
public class PostBookmarkController {

    private final PostBookmarkService postBookmarkService;

    /**
     * 포스팅 즐겨찾기 API. 멱등하다.
     */
    @Operation(summary = "즐겨찾기 등록")
    @PostMapping("/postings/{postId}/bookmarks")
    public ResponseEntity<Void> bookmark(@PathVariable UUID postId,
                                         @CurrentUser JwtPrincipal principal) {

        postBookmarkService.bookmark(principal.getUserId(), postId);
        return ResponseEntity.accepted().build();
    }

    /**
     * 포스팅 즐겨찾기 취소 API. 멱등하다.
     */
    @Operation(summary = "즐겨찾기 삭제")
    @DeleteMapping("/postings/{postId}/bookmarks")
    public ResponseEntity<Void> unBookmark(@PathVariable UUID postId,
                                           @CurrentUser JwtPrincipal principal) {

        postBookmarkService.unBookmark(principal.getUserId(), postId);
        return ResponseEntity.accepted().build();
    }

    /**
     * 내 즐겨찾기 목록 조회. 북마크 등록 시각 내림차순으로 페이지네이션한다.
     */
    @Operation(summary = "내 즐겨찾기 목록 조회")
    @GetMapping("/me/bookmarks")
    public ResponseEntity<OffsetPageResult<PostDocument>> getMyBookmarks(@PageableDefault(size = 20) Pageable pageable,
                                                                         @CurrentUser JwtPrincipal principal) {

        OffsetPageResult<PostDocument> result = postBookmarkService.getMyBookmarks(principal.getUserId(), pageable);
        return ResponseEntity.ok(result);
    }
}
