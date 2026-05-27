package com.backend.integratedapi.indexedpost.service.dto;

import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record IndexedPost(UUID id,
                          String title,
                          String url,
                          String thumbnailUrl,
                          String summary,
                          String provider,
                          PostStatus status,
                          LocalDate publishedAt,
                          OffsetDateTime createdAt,
                          OffsetDateTime updatedAt) {

}
