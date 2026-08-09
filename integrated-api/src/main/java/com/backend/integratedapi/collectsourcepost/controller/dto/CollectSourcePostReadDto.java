package com.backend.integratedapi.collectsourcepost.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.IndexingStatus;
import com.backend.integratedapi.collectsourcepost.service.dto.CollectSourcePostDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CollectSourcePostReadDto() {

    @Schema(description = "수집된 게시글 조회 결과")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record Response(
            @Schema(description = "게시글 ID")
            UUID postingId,

            @Schema(description = "수집 소스 ID")
            UUID collectSourceId,

            @Schema(description = "게시글 제목")
            String title,

            @Schema(description = "원문 URL")
            String url,

            @Schema(description = "원문 발행일")
            LocalDate publishedAt,

            @Schema(description = "썸네일 이미지 URL")
            String thumbnailUrl,

            @Schema(description = "본문 요약")
            String summary,

            @Schema(description = "색인 상태")
            IndexingStatus indexingStatus,

            @Schema(description = "색인 실패 누적 횟수")
            int indexingErrorCount,

            @Schema(description = "마지막 색인 시각")
            OffsetDateTime lastIndexedAt,

            @Schema(description = "마지막 수집 시각")
            OffsetDateTime lastCollectedAt,

            @Schema(description = "마지막 수집 Job ID")
            UUID lastCollectingJobId) {

        public static Response from(CollectSourcePostDto collectSourcePostDto) {
            return CollectSourcePostReadDto.Response.builder()
                                                    .postingId(collectSourcePostDto.id())
                                                    .collectSourceId(collectSourcePostDto.collectSourceId())
                                                    .title(collectSourcePostDto.title())
                                                    .url(collectSourcePostDto.url())
                                                    .publishedAt(collectSourcePostDto.publishedAt())
                                                    .thumbnailUrl(collectSourcePostDto.thumbnailUrl())
                                                    .summary(collectSourcePostDto.summary())
                                                    .indexingStatus(collectSourcePostDto.indexingStatus())
                                                    .indexingErrorCount(collectSourcePostDto.indexingErrorCount())
                                                    .lastIndexedAt(collectSourcePostDto.lastIndexedAt())
                                                    .lastCollectedAt(collectSourcePostDto.lastCollectedAt())
                                                    .lastCollectingJobId(collectSourcePostDto.lastCollectingJobId())
                                                    .build();
        }
    }
}
