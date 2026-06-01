package com.backend.interactionservice.post.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.interactionservice.post.service.dto.PostDocument;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PostDocumentValidator {

    public static void validateId(UUID id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new BadRequestException("id는 필수 입력값입니다.");
        }
    }

    public static PostDocument getPostDocumentOrThrow(UUID id,
                                                      Function<UUID, Optional<PostDocument>> fetchOneById) {

        validateId(id);

        return fetchOneById.apply(id)
                           .orElseThrow(() -> new NotFoundException(String.format("존재하지 않는 PostDocument입니다. id: %s", id)));
    }
}
