package com.backend.integratedworker.collectingjob.service.validator;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.apache.commons.lang3.ObjectUtils;

public class CollectingJobValidator {

    public static void validateId(UUID id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("id는 필수 입력값입니다.");
        }
    }

    public static CollectingJob getCollectingJobOrThrow(UUID id, Function<UUID, Optional<CollectingJob>> fetchOneById) {
        validateId(id);

        return fetchOneById.apply(id)
                       .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 collecting_job입니다. id: " + id));
    }
}
