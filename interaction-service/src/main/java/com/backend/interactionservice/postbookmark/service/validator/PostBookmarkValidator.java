package com.backend.interactionservice.postbookmark.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostBookmarkValidator {

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
}
