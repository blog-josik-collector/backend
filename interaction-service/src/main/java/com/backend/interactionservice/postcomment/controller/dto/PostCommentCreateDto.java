package com.backend.interactionservice.postcomment.controller.dto;

import com.backend.interactionservice.postcomment.service.dto.PostCommentDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostCommentCreateDto() {

    @Schema(description = "댓글/대댓글 작성 요청")
    public record Request(
            @Schema(description = "댓글 본문", example = "좋은 글 감사합니다.")
            String content) {

    }

    // '댓글 작성'용 응답
    @Schema(description = "댓글 작성 결과")
    @Builder
    public record Response(
            @Schema(description = "생성된 댓글 ID")
            UUID id,

            @Schema(description = "작성 시각")
            OffsetDateTime createdAt) {

        public static Response from(PostCommentDto postCommentDto) {
            return Response.builder()
                           .id(postCommentDto.id())
                           .createdAt(postCommentDto.createdAt())
                           .build();
        }
    }

    // '대댓글 작성'용 응답
    @Schema(description = "대댓글 작성 결과")
    @Builder
    public record ReplyResponse(
            @Schema(description = "생성된 대댓글 ID")
            UUID id,

            @Schema(description = "부모 댓글 ID. 대댓글의 부모는 항상 1-depth 댓글이다")
            UUID parentId,

            @Schema(description = "작성 시각")
            OffsetDateTime createdAt) {

        public static ReplyResponse of(UUID parentId, PostCommentDto postCommentDto) {
            return ReplyResponse.builder()
                           .id(postCommentDto.id())
                           .parentId(parentId)
                           .createdAt(postCommentDto.createdAt())
                           .build();
        }
    }
}
