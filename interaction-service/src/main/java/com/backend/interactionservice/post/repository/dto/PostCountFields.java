package com.backend.interactionservice.post.repository.dto;

import com.backend.commondataaccess.persistence.post.Post;

/**
 * Elasticsearch techblog-posts-v1 인덱스에 partial upsert 할 카운트 필드만 담는 DTO.
 */
public record PostCountFields(Integer likeCount,
                              Integer viewCount,
                              Integer commentCount,
                              Integer totalReportCount) {

    public static PostCountFields from(Post post) {
        return new PostCountFields(post.likeCount(),
                                   post.viewCount(),
                                   post.commentCount(),
                                   post.totalReportCount());
    }
}
