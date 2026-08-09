package com.backend.interactionservice.postcomment.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.PostCommentStatus;
import com.backend.interactionservice.postcomment.service.dto.PostCommentDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostCommentReadDto() {

    @Schema(description = "댓글/대댓글 조회 결과")
    @Builder
    public record Response(
            @Schema(description = "댓글 ID")
            UUID id,

            @Schema(description = "작성자 ID")
            UUID userId,

            @Schema(description = "대댓글 보유 여부. true 이면 replies 조회 API로 하위 목록을 가져올 수 있다")
            boolean hasChildComment,

            @Schema(description = "댓글 본문. 삭제된 댓글은 status 로 구분한다")
            String content,

            @Schema(description = "댓글 상태")
            PostCommentStatus status,

            @Schema(description = "작성 시각")
            OffsetDateTime createdAt,

            @Schema(description = "최종 수정 시각")
            OffsetDateTime updatedAt) {

        public static Response from(PostCommentDto postCommentDto) {
            return Response.builder()
                           .id(postCommentDto.id())
                           .userId(postCommentDto.userId())
                           .hasChildComment(postCommentDto.hasChildComment())
                           .content(postCommentDto.content())
                           .status(postCommentDto.status())
                           .createdAt(postCommentDto.createdAt())
                           .updatedAt(postCommentDto.updatedAt())
                           .build();
        }
    }
}
