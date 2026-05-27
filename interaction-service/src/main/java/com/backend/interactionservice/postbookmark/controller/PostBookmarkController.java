package com.backend.interactionservice.postbookmark.controller;

import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.interactionservice.postbookmark.service.PostBookmarkService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "03. 포스팅 즐겨찾기 API")
@RequestMapping("/interaction/v1")
@RestController
@RequiredArgsConstructor
public class PostBookmarkController {

    private final PostBookmarkService postBookmarkService;

    /**
     * 포스팅 즐겨찾기 API. 멱등하다.
     */
    @PostMapping("/postings/{postId}/bookmarks")
    public ResponseEntity<Void> bookmark(@PathVariable UUID postId,
                                         @AuthenticationPrincipal JwtPrincipal principal) {

        postBookmarkService.bookmark(principal.getUserId(), postId);
        return ResponseEntity.accepted().build();
    }

    /**
     * 포스팅 즐겨찾기 취소 API. 멱등하다.
     */
    @DeleteMapping("/postings/{postId}/bookmarks")
    public ResponseEntity<Void> unBookmark(@PathVariable UUID postId,
                                           @AuthenticationPrincipal JwtPrincipal principal) {

        postBookmarkService.unBookmark(principal.getUserId(), postId);
        return ResponseEntity.accepted().build();
    }
}
