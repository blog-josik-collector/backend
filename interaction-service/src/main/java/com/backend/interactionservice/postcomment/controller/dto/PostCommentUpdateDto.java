package com.backend.interactionservice.postcomment.controller.dto;

import com.backend.interactionservice.postcomment.service.dto.PostCommentDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostCommentUpdateDto() {

    public record Request(String content) {
    }

    @Builder
    public record Response(UUID id,
                           OffsetDateTime updatedAt) {

        public static Response from(PostCommentDto postCommentDto) {
            return Response.builder()
                           .id(postCommentDto.id())
                           .updatedAt(postCommentDto.updatedAt())
                           .build();
        }
    }
}
