package com.backend.interactionservice.post.service.dto;

import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PostListItem(UUID id,
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
                           Integer totalReportCount,
                           boolean likesOfMe,
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
