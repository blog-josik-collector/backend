package com.backend.interactionservice.user.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.user.User;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UserValidator {

    public static void validateId(UUID id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new BadRequestException("id는 필수 입력값입니다.");
        }
    }

    public static User getUserOrThrow(UUID id, Function<UUID, Optional<User>> fetchOneById) {
        validateId(id);

        return fetchOneById.apply(id)
                           .orElseThrow(() -> new NotFoundException("존재하지 않는 user입니다. id: " + id));
    }
}
