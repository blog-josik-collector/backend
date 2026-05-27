package com.backend.interactionservice.post.service.dto;

import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PostDocument(UUID id,
                           String title,
                           String url,
                           String thumbnailUrl,
                           String summary,
                           String provider,
                           PostStatus status,
                           LocalDate publishedAt,
                           OffsetDateTime createdAt,
                           OffsetDateTime updatedAt,
                           Integer likeCount,
                           Integer viewCount,
                           Integer commentCount,
                           Integer totalReportCount) {

}
