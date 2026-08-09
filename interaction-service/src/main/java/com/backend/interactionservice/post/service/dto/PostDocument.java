package com.backend.interactionservice.post.service.dto;

import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "색인된 기술 블로그 포스팅 문서")
public record PostDocument(
        @Schema(description = "포스팅 ID", example = "e00be928-87b4-4b07-9b2f-410a8d4b32b1")
        UUID id,

        @Schema(description = "포스팅 제목", example = "Spring Boot에서 Clean Architecture 적용하기")
        String title,

        @Schema(description = "원문 링크", example = "https://techblog.com/clean-architecture")
        String url,

        @Schema(description = "썸네일 이미지 URL")
        String thumbnailUrl,

        @Schema(description = "본문 요약")
        String summary,

        @Schema(description = "블로그 제공자", example = "Toss")
        String provider,

        @Schema(description = "게시 상태")
        PostStatus status,

        @Schema(description = "원문 발행일", example = "2026-07-20")
        LocalDate publishedAt,

        @Schema(description = "수집 시각")
        OffsetDateTime createdAt,

        @Schema(description = "최종 수정 시각")
        OffsetDateTime updatedAt,

        @Schema(description = "좋아요 수", example = "15")
        Integer likeCount,

        @Schema(description = "조회수", example = "120")
        Integer viewCount,

        @Schema(description = "댓글 수(대댓글 포함)", example = "3")
        Integer commentCount,

        @Schema(description = "누적 신고 수", example = "0")
        Integer totalReportCount) {

}
