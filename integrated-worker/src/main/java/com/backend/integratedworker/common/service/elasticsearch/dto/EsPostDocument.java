package com.backend.integratedworker.common.service.elasticsearch.dto;

import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import java.time.LocalDate;
import java.util.UUID;

public record EsPostDocument(UUID id,
                             String title,
                             String url,
                             String thumbnailUrl,
                             String summary,
                             LocalDate publishedAt,
                             String provider) {

    public static EsPostDocument from(CollectSourcePost collectSourcePost) {
        return new EsPostDocument(collectSourcePost.id(),
                                  collectSourcePost.title(),
                                  collectSourcePost.url(),
                                  collectSourcePost.thumbnailUrl(),
                                  collectSourcePost.summary(),
                                  collectSourcePost.publishedAt(),
                                  collectSourcePost.lastCollectingJob().collectSource().postProvider().name());
    }
}
