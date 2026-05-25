package com.backend.integratedworker.common.service.elasticsearch.dto;

import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.common.enums.DocumentStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record EsPostDocument(UUID id,
                             String title,
                             String url,
                             String thumbnailUrl,
                             String summary,
                             String provider,
                             DocumentStatus status,
                             LocalDate publishedAt,
                             OffsetDateTime createdAt,
                             OffsetDateTime updatedAt) {

    public static EsPostDocument from(CollectSourcePost collectSourcePost) {
        return new EsPostDocument(collectSourcePost.id(),
                                  collectSourcePost.title(),
                                  collectSourcePost.url(),
                                  collectSourcePost.thumbnailUrl(),
                                  collectSourcePost.summary(),
                                  collectSourcePost.collectSource().postProvider().name(),
                                  DocumentStatus.ACTIVE,
                                  collectSourcePost.publishedAt(),
                                  collectSourcePost.createdAt(),
                                  collectSourcePost.updatedAt());
    }
}
