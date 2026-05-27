package com.backend.interactionservice.postcomment.service.dto;

import com.backend.commondataaccess.persistence.common.enums.PostCommentStatus;
import com.backend.commondataaccess.persistence.post.PostComment;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PostCommentDto(UUID id,
                             UUID postId,
                             UUID parentCommentId,
                             UUID userId,
                             String nickname,
                             String content,
                             PostCommentStatus status,
                             boolean reply,
                             boolean hasChildComment,
                             OffsetDateTime createdAt,
                             OffsetDateTime updatedAt) {

    /**
     * hasChildComment 정보를 알 수 없는 호출 지점(작성, 수정 직후 등)에서 사용. 기본값은 false.
     */
    public static PostCommentDto from(PostComment comment) {
        return from(comment, false);
    }

    /**
     * 목록 조회 등 자식 댓글 존재 여부를 함께 계산한 호출 지점에서 사용.
     */
    public static PostCommentDto from(PostComment comment, boolean hasChildComment) {
        return new PostCommentDto(
                comment.id(),
                comment.post() != null ? comment.post().id() : null,
                comment.parentComment() != null ? comment.parentComment().id() : null,
                comment.user() != null ? comment.user().id() : null,
                comment.user() != null ? comment.user().nickname() : null,
                comment.content(),
                comment.postCommentStatus(),
                comment.isReplyComment(),
                hasChildComment,
                comment.createdAt(),
                comment.updatedAt()
        );
    }
}
