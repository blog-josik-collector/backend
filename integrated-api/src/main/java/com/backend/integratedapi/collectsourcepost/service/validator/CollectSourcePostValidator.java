package com.backend.integratedapi.collectsourcepost.service.validator;

import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CollectSourcePostValidator {

    public static void validateId(UUID id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("id는 필수 입력값입니다.");
        }
    }

    public static CollectSourcePost getCollectSourcePostOrThrow(UUID id, Function<UUID, Optional<CollectSourcePost>> fetchOneById) {
        validateId(id);
        return fetchOneById.apply(id)
                           .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 id입니다. id: " + id));
    }
}
