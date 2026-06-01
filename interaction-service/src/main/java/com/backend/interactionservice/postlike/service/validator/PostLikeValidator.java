package com.backend.interactionservice.postlike.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.post.PostLike;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostLikeValidator {

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

    public static PostLike getPostLikeOrThrow(UUID userId,
                                              UUID postId,
                                              BiFunction<UUID, UUID, Optional<PostLike>> fetchOneByUserAndPost) {

        validateUserId(userId);
        validatePostId(postId);

        return fetchOneByUserAndPost.apply(userId, postId)
                                    .orElseThrow(() -> new NotFoundException(String.format("존재하지 않는 postLike입니다. userId: %s, postId: %s", userId, postId)));
    }
}
