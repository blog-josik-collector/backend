package com.backend.integratedapi.collectsourcepost.service.dto;

import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.common.enums.IndexingStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(fluent = true)
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더용
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // Jackson 역직렬화용
public class CollectSourcePostDto {

    private UUID id;

    private UUID collectSourceId;

    private String title;

    private String url;

    private LocalDate publishedAt; // 발행일

    private String thumbnailUrl;  // 썸네일 이미지

    private String summary; // 요약

    private IndexingStatus indexingStatus;

    private int indexingErrorCount;

    private OffsetDateTime lastIndexedAt;

    private OffsetDateTime lastCollectedAt;

    private UUID lastCollectingJobId;

    public static CollectSourcePostDto from(CollectSourcePost collectSourcePost) {
        return CollectSourcePostDto.builder()
                                   .id(collectSourcePost.id())
                                   .collectSourceId(collectSourcePost.collectSource().id())
                                   .title(collectSourcePost.title())
                                   .url(collectSourcePost.url())
                                   .publishedAt(collectSourcePost.publishedAt())
                                   .thumbnailUrl(collectSourcePost.thumbnailUrl())
                                   .summary(collectSourcePost.summary())
                                   .indexingStatus(collectSourcePost.indexingStatus())
                                   .indexingErrorCount(collectSourcePost.indexingErrorCount())
                                   .lastIndexedAt(collectSourcePost.lastIndexedAt())
                                   .lastCollectedAt(collectSourcePost.lastCollectedAt())
                                   .lastCollectingJobId(collectSourcePost.lastCollectingJob().id())
                                   .build();
    }
}
