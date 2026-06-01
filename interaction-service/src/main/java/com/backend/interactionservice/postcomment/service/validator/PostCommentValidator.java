package com.backend.interactionservice.postcomment.service.validator;

import com.backend.commondataaccess.exception.AccessDeniedException;
import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.post.PostComment;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PostCommentValidator {

    private static final int MAX_CONTENT_LENGTH = 1000;

    public static void validateUserId(UUID userId) {
        if (ObjectUtils.isEmpty(userId)) {
            throw new BadRequestException("userId는 필수 입력값입니다.");
        }
    }

    public static void validatePostId(UUID postId) {
        if (ObjectUtils.isEmpty(postId)) {
            throw new BadRequestException("postId는 필수 입력값입니다.");
        }
    }

    public static void validateCommentId(UUID commentId) {
        if (ObjectUtils.isEmpty(commentId)) {
            throw new BadRequestException("commentId는 필수 입력값입니다.");
        }
    }

    public static void validateContent(String content) {
        if (StringUtils.isBlank(content)) {
            throw new BadRequestException("댓글 내용은 비어있을 수 없습니다.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BadRequestException(String.format("댓글 내용은 %d자를 초과할 수 없습니다.", MAX_CONTENT_LENGTH));
        }
    }

    public static void validateOwnership(PostComment comment, UUID userId) {
        validateUserId(userId);
        if (!comment.isOwnedBy(userId)) {
            throw new AccessDeniedException("댓글에 대한 권한이 없습니다.");
        }
    }

    /**
     * 1-depth 댓글 라우트에서 호출. 대상이 대댓글이면 잘못된 라우팅이므로 거부.
     */
    public static void validateIsComment(PostComment comment) {
        if (comment.isReplyComment()) {
            throw new BadRequestException("해당 리소스는 대댓글입니다. 대댓글 API를 사용해야 합니다.");
        }
    }

    /**
     * 2-depth 대댓글 라우트에서 호출. 대상이 일반 댓글이면 잘못된 라우팅이므로 거부.
     */
    public static void validateIsReply(PostComment comment) {
        if (!comment.isReplyComment()) {
            throw new BadRequestException("해당 리소스는 댓글입니다. 댓글 API를 사용해야 합니다.");
        }
    }

    public static PostComment getPostCommentOrThrow(UUID commentId, Function<UUID, Optional<PostComment>> fetchOneById) {
        validateCommentId(commentId);

        return fetchOneById.apply(commentId)
                           .orElseThrow(() -> new NotFoundException("존재하지 않는 댓글입니다. id: " + commentId));
    }
}
