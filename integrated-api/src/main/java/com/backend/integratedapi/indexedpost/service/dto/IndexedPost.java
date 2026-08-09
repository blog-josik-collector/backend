package com.backend.integratedapi.indexedpost.service.dto;

import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "색인된 기술 블로그 포스팅")
public record IndexedPost(
        @Schema(description = "포스팅 ID")
        UUID id,

        @Schema(description = "포스팅 제목")
        String title,

        @Schema(description = "원문 링크")
        String url,

        @Schema(description = "썸네일 이미지 URL")
        String thumbnailUrl,

        @Schema(description = "본문 요약")
        String summary,

        @Schema(description = "블로그 제공자", example = "Toss")
        String provider,

        @Schema(description = "게시 상태")
        PostStatus status,

        @Schema(description = "원문 발행일")
        LocalDate publishedAt,

        @Schema(description = "수집/생성 시각")
        OffsetDateTime createdAt,

        @Schema(description = "최종 수정 시각")
        OffsetDateTime updatedAt) {

}
