package com.backend.integratedworker.collectingjob.service.validator;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CollectingJobValidator 테스트")
class CollectingJobValidatorTest {

    @Nested
    @DisplayName("필수값 검증")
    class RequiredFields {

        @Test
        void id가_null이면_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(() -> CollectingJobValidator.validateId(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id");
        }

        @Test
        void id가_있으면_통과한다() {
            Assertions.assertThatCode(() -> CollectingJobValidator.validateId(UUID.randomUUID()))
                      .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("getCollectingJobOrThrow")
    class GetCollectingJobOrThrow {

        @Test
        void 존재하면_CollectingJob을_반환한다() {
            UUID id = UUID.randomUUID();
            CollectingJob collectingJob = CollectingJob.builder()
                                                       .id(id)
                                                       .jobStatus(JobStatus.PENDING)
                                                       .build();
            Function<UUID, Optional<CollectingJob>> fetch = ignored -> Optional.of(collectingJob);

            CollectingJob found = CollectingJobValidator.getCollectingJobOrThrow(id, fetch);

            Assertions.assertThat(found).isSameAs(collectingJob);
        }

        @Test
        void 없으면_NotFoundException을_던진다() {
            UUID id = UUID.randomUUID();

            Assertions.assertThatThrownBy(
                              () -> CollectingJobValidator.getCollectingJobOrThrow(id, ignored -> Optional.empty()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("collecting_job");
        }

        @Test
        void id가_null이면_조회하지_않고_BadRequestException을_던진다() {
            Assertions.assertThatThrownBy(
                              () -> CollectingJobValidator.getCollectingJobOrThrow(null, ignored -> {
                                  throw new AssertionError("조회되면 안 된다");
                              }))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id");
        }
    }
}
