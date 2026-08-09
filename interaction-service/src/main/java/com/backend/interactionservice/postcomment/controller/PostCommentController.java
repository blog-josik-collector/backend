package com.backend.interactionservice.postcomment.controller;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.security.CurrentUser;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.interactionservice.postcomment.controller.dto.PostCommentCreateDto;
import com.backend.interactionservice.postcomment.controller.dto.PostCommentReadDto;
import com.backend.interactionservice.postcomment.controller.dto.PostCommentUpdateDto;
import com.backend.interactionservice.postcomment.service.PostCommentService;
import com.backend.interactionservice.postcomment.service.dto.PostCommentDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "04. 포스팅 댓글/대댓글 API")
@RequestMapping("/interaction/v1")
@RestController
@RequiredArgsConstructor
public class PostCommentController {

    private final PostCommentService postCommentService;

    /**
     * 1. 댓글 작성.
     */
    @Operation(summary = "댓글 작성")
    @PostMapping("/postings/{postId}/comments")
    public ResponseEntity<PostCommentCreateDto.Response> createComment(@PathVariable UUID postId,
                                                                       @RequestBody PostCommentCreateDto.Request request,
                                                                       @CurrentUser JwtPrincipal principal) {

        PostCommentDto postCommentDto = postCommentService.createComment(principal.getUserId(), postId, request.content());
        PostCommentCreateDto.Response response = PostCommentCreateDto.Response.from(postCommentDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 2. 포스팅의 댓글(1-depth) 목록 조회. 생성 순(오래된 순)으로 페이지네이션.
     */
    @Operation(summary = "댓글 목록 조회")
    @GetMapping("/postings/{postId}/comments")
    public ResponseEntity<OffsetPageResult<PostCommentReadDto.Response>> getComments(@PathVariable UUID postId,
                                                                                     @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(postCommentService.getComments(postId, pageable).map(PostCommentReadDto.Response::from));
    }

    /**
     * 3. 댓글 수정. 본인만 가능.
     */
    @Operation(summary = "댓글 수정")
    @PatchMapping("/comments/{commentId}")
    public ResponseEntity<PostCommentUpdateDto.Response> updateComment(@PathVariable UUID commentId,
                                                                       @RequestBody PostCommentUpdateDto.Request request,
                                                                       @CurrentUser JwtPrincipal principal) {

        PostCommentDto postCommentDto = postCommentService.updateComment(principal.getUserId(), commentId, request.content());
        PostCommentUpdateDto.Response response = PostCommentUpdateDto.Response.from(postCommentDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 4. 댓글 삭제. 본인만 가능. soft-delete.
     */
    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable UUID commentId,
                                              @CurrentUser JwtPrincipal principal) {

        postCommentService.deleteComment(principal.getUserId(), commentId);
        return ResponseEntity.accepted().build();
    }

    /**
     * 5. 대댓글 작성. 부모는 1-depth 댓글이어야 한다.
     */
    @Operation(summary = "대댓글 작성")
    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<PostCommentCreateDto.ReplyResponse> createReply(@PathVariable UUID commentId,
                                                                          @RequestBody PostCommentCreateDto.Request request,
                                                                          @CurrentUser JwtPrincipal principal) {

        PostCommentDto postCommentDto = postCommentService.createReply(principal.getUserId(), commentId, request.content());
        PostCommentCreateDto.ReplyResponse response = PostCommentCreateDto.ReplyResponse.of(commentId, postCommentDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 6. 특정 댓글의 대댓글 목록 조회. 생성 순(오래된 순)으로 페이지네이션.
     */
    @Operation(summary = "대댓글 조회")
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<OffsetPageResult<PostCommentReadDto.Response>> getReplies(@PathVariable UUID commentId,
                                                                                    @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(postCommentService.getReplies(commentId, pageable).map(PostCommentReadDto.Response::from));
    }

    /**
     * 7. 대댓글 수정. 본인만 가능.
     */
    @Operation(summary = "대댓글 수정")
    @PatchMapping("/replies/{replyId}")
    public ResponseEntity<PostCommentUpdateDto.Response> updateReply(@PathVariable UUID replyId,
                                                                     @RequestBody PostCommentUpdateDto.Request request,
                                                                     @CurrentUser JwtPrincipal principal) {

        PostCommentDto postCommentDto = postCommentService.updateReply(principal.getUserId(), replyId, request.content());
        PostCommentUpdateDto.Response response = PostCommentUpdateDto.Response.from(postCommentDto);
        return ResponseEntity.ok(response);
    }

    /**
     * 8. 대댓글 삭제. 본인만 가능. soft-delete.
     */
    @Operation(summary = "대댓글 삭제")
    @DeleteMapping("/replies/{replyId}")
    public ResponseEntity<Void> deleteReply(@PathVariable UUID replyId,
                                            @CurrentUser JwtPrincipal principal) {

        postCommentService.deleteReply(principal.getUserId(), replyId);
        return ResponseEntity.accepted().build();
    }

    /**
     * 9. 내가 작성한 댓글/대댓글 조회. 최신 순으로 페이지네이션.
     */
    @Operation(summary = "내가 작성한 댓글/대댓글 조회")
    @GetMapping("/me/comments")
    public ResponseEntity<OffsetPageResult<PostCommentReadDto.Response>> getMyComments(@PageableDefault(size = 20) Pageable pageable,
                                                                                       @CurrentUser JwtPrincipal principal) {

        return ResponseEntity.ok(postCommentService.getMyComments(principal.getUserId(), pageable).map(PostCommentReadDto.Response::from));
    }
}
