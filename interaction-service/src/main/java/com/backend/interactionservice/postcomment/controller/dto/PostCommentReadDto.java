package com.backend.interactionservice.postcomment.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.PostCommentStatus;
import com.backend.interactionservice.postcomment.service.dto.PostCommentDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostCommentReadDto() {

    @Builder
    public record Response(UUID id,
                           UUID userId,
                           boolean hasChildComment,
                           String content,
                           PostCommentStatus status,
                           OffsetDateTime createdAt,
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
