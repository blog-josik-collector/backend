package com.backend.integratedapi.collectsourcepost.controller.dto;

import com.backend.commondataaccess.persistence.common.enums.IndexingStatus;
import com.backend.integratedapi.collectsourcepost.service.dto.CollectSourcePostDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record CollectSourcePostReadDto() {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record Response(UUID postingId,
                           UUID collectSourceId,
                           String title,
                           String url,
                           LocalDate publishedAt,
                           String thumbnailUrl,
                           String summary,
                           IndexingStatus indexingStatus,
                           int indexingErrorCount,
                           OffsetDateTime lastIndexedAt,
                           OffsetDateTime lastCollectedAt,
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
