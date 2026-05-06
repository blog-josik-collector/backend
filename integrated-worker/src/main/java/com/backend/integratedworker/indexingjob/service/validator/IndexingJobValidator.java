package com.backend.integratedworker.indexingjob.service.validator;

import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class IndexingJobValidator {

    public static void validateId(UUID id) {
        if (ObjectUtils.isEmpty(id)) {
            throw new IllegalArgumentException("id는 필수 입력값입니다.");
        }
    }

    public static IndexingJob getIndexingJobOrThrow(UUID id, Function<UUID, Optional<IndexingJob>> fetchOneById) {
        validateId(id);

        return fetchOneById.apply(id)
                           .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 indexingJob입니다. id: " + id));
    }
}
