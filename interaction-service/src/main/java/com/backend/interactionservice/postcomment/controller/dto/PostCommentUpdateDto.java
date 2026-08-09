package com.backend.interactionservice.postcomment.controller.dto;

import com.backend.interactionservice.postcomment.service.dto.PostCommentDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostCommentUpdateDto() {

    @Schema(description = "댓글/대댓글 수정 요청")
    public record Request(
            @Schema(description = "수정할 댓글 본문", example = "내용을 수정했습니다.")
            String content) {
    }

    @Schema(description = "댓글/대댓글 수정 결과")
    @Builder
    public record Response(
            @Schema(description = "수정된 댓글 ID")
            UUID id,

            @Schema(description = "수정 시각")
            OffsetDateTime updatedAt) {

        public static Response from(PostCommentDto postCommentDto) {
            return Response.builder()
                           .id(postCommentDto.id())
                           .updatedAt(postCommentDto.updatedAt())
                           .build();
        }
    }
}
