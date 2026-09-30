package com.backend.integratedworker.indexingjob.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("IndexingJobValidator 테스트")
class IndexingJobValidatorTest {

    @Nested
    @DisplayName("필수값 검증")
    class RequiredFields {

        @Test
        void id가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> IndexingJobValidator.validateId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id");
        }

        @Test
        void id가_있으면_통과한다() {
            Assertions.assertThatCode(() -> IndexingJobValidator.validateId(UUID.randomUUID()))
                      .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getIndexingJobOrThrow")
    class GetIndexingJobOrThrow {

        @Test
        void 존재하면_IndexingJob을_반환한다() {
            UUID id = UUID.randomUUID();
            IndexingJob indexingJob = IndexingJob.builder()
                                                 .id(id)
                                                 .indexingJobType(IndexingJobType.MANUAL)
                                                 .jobStatus(JobStatus.PENDING)
                                                 .build();
            Function<UUID, Optional<IndexingJob>> fetch = ignored -> Optional.of(indexingJob);

            IndexingJob found = IndexingJobValidator.getIndexingJobOrThrow(id, fetch);

            Assertions.assertThat(found).isSameAs(indexingJob);
        }

        @Test
        void 없으면_NotFoundException을_던진다() {
            UUID id = UUID.randomUUID();

            Assertions.assertThatThrownBy(
                              () -> IndexingJobValidator.getIndexingJobOrThrow(id, ignored -> Optional.empty()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("indexingJob");
        }
    }
}
