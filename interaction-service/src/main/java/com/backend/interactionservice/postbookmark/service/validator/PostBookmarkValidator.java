package com.backend.interactionservice.postbookmark.service.validator;

import com.backend.commondataaccess.persistence.post.PostBookmark;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostBookmarkValidator {

    public static void validateUserId(UUID userId) {
        if (ObjectUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("userId는 필수 입력값입니다.");
        }
    }

    public static void validatePostId(UUID postId) {
        if (ObjectUtils.isEmpty(postId)) {
            throw new IllegalArgumentException("postId는 필수 입력값입니다.");
        }
    }

    public static PostBookmark getPostBookmarkOrThrow(UUID userId,
                                                      UUID postId,
                                                      BiFunction<UUID, UUID, Optional<PostBookmark>> fetchOneByUserAndPost) {

        validateUserId(userId);
        validatePostId(postId);

        return fetchOneByUserAndPost.apply(userId, postId)
                                    .orElseThrow(() -> new IllegalArgumentException(String.format("존재하지 않는 postBookmark입니다. userId: %s, postId: %s", userId, postId)));
    }
}
