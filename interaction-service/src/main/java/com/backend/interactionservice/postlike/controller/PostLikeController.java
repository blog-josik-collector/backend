package com.backend.interactionservice.postlike.controller;

import com.backend.commondataaccess.security.CurrentUser;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.interactionservice.postlike.service.PostLikeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "02. 포스팅 좋아요 API")
@RequestMapping("/interaction/v1")
@RestController
@RequiredArgsConstructor
public class PostLikeController {

    private final PostLikeService postLikeService;

    /**
     * 포스팅 좋아요 API. 멱등하므로 여러 번 호출되어도 like_count는 한 번만 증가한다.
     */
    @Operation(summary = "좋아요 등록")
    @PostMapping("/postings/{postId}/likes")
    public ResponseEntity<Void> like(@PathVariable UUID postId,
                                     @CurrentUser JwtPrincipal principal) {
        postLikeService.like(principal.getUserId(), postId);
        return ResponseEntity.accepted().build();
    }

    /**
     * 포스팅 좋아요 취소 API. 멱등하므로 여러 번 호출되어도 like_count는 한 번만 감소한다.
     */
    @Operation(summary = "좋아요 취소")
    @DeleteMapping("/postings/{postId}/likes")
    public ResponseEntity<Void> unLike(@PathVariable UUID postId,
                                       @CurrentUser JwtPrincipal principal) {
        postLikeService.unLike(principal.getUserId(), postId);
        return ResponseEntity.accepted().build();
    }
}
