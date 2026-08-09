package com.backend.interactionservice.post.service.dto;

import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PostListItem(
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
        @Schema(description = "수집/생성 시각")
        OffsetDateTime createdAt,
        @Schema(description = "최종 수정 시각")
        OffsetDateTime updatedAt,
        @Schema(description = "좋아요 수", example = "15")
        Integer likeCount,
        @Schema(description = "조회수. Redis 누적 후 PostViewCountFlushWorker 가 주기적으로 DB 반영", example = "120")
        Integer viewCount,
        @Schema(description = "댓글 수(대댓글 포함)", example = "3")
        Integer commentCount,
        @Schema(description = "누적 신고 수", example = "0")
        Integer totalReportCount,
        @Schema(description = "현재 로그인 사용자의 좋아요 여부. 미인증 호출 시 항상 false")
        boolean likesOfMe,
        @Schema(description = "현재 로그인 사용자의 즐겨찾기 여부. 미인증 호출 시 항상 false")
        boolean bookmarksOfMe) {

    public static PostListItem of(PostDocument doc, boolean likesOfMe, boolean bookmarksOfMe) {
        return new PostListItem(doc.id(),
                                doc.title(),
                                doc.url(),
                                doc.thumbnailUrl(),
                                doc.summary(),
                                doc.provider(),
                                doc.status(),
                                doc.publishedAt(),
                                doc.createdAt(),
                                doc.updatedAt(),
                                doc.likeCount(),
                                doc.viewCount(),
                                doc.commentCount(),
                                doc.totalReportCount(),
                                likesOfMe,
                                bookmarksOfMe);
    }
}
