package com.backend.interactionservice.postcomment.controller.dto;

import com.backend.interactionservice.postcomment.service.dto.PostCommentDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostCommentCreateDto() {

    public record Request(String content) {

    }

    // '댓글 작성'용 응답
    @Builder
    public record Response(UUID id,
                           OffsetDateTime createdAt) {

        public static Response from(PostCommentDto postCommentDto) {
            return Response.builder()
                           .id(postCommentDto.id())
                           .createdAt(postCommentDto.createdAt())
                           .build();
        }
    }

    // '대댓글 작성'용 응답
    @Builder
    public record ReplyResponse(UUID id,
                                UUID parentId,
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
