package com.backend.integratedworker.collectingjob.service;

import static org.mockito.ArgumentMatchers.anyInt;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.common.enums.ScheduleType;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.integratedworker.collectingjob.repository.CollectingJobRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("CollectingJobPicker 테스트")
@ExtendWith(MockitoExtension.class)
class CollectingJobPickerTest {

    @Spy
    @InjectMocks
    private CollectingJobPicker collectingJobPicker;

    @Mock
    private CollectingJobRepository collectingJobRepository;

    private CollectSource collectSource;

    @BeforeEach
    void init() {
        PostProvider postProvider = PostProvider.builder()
                                                .id(UUID.randomUUID())
                                                .name("test_provider")
                                                .baseUrl("https://test.com")
                                                .description("test_description")
                                                .isUsed(true)
                                                .collectSources(new ArrayList<>())
                                                .build();

        collectSource = CollectSource.builder()
                                     .id(UUID.randomUUID())
                                     .postProvider(postProvider)
                                     .url("https://test.com/blog/1")
                                     .scheduleType(ScheduleType.MANUAL)
                                     .isUsed(true)
                                     .build();
    }

    private CollectingJob pendingJob() {
        return CollectingJob.builder()
                            .id(UUID.randomUUID())
                            .collectSource(collectSource)
                            .jobStatus(JobStatus.PENDING)
                            .build();
    }

    @DisplayName("PENDING Job pick 테스트")
    @Nested
    class PickAndMarkRunningTest {

        @Test
        void PENDING_상태의_Job들을_pick하고_RUNNING으로_마킹한다() {
            // given
            CollectingJob job1 = pendingJob();
            CollectingJob job2 = pendingJob();

            Mockito.doReturn(List.of(job1, job2)).when(collectingJobRepository).pickPending(anyInt());

            Assertions.assertThat(job1.jobStatus()).isEqualTo(JobStatus.PENDING);
            Assertions.assertThat(job2.jobStatus()).isEqualTo(JobStatus.PENDING);

            // when
            List<UUID> result = collectingJobPicker.pickAndMarkRunning(10);

            // then
            Assertions.assertThat(result).containsExactlyInAnyOrder(job1.id(), job2.id());
            Assertions.assertThat(job1.jobStatus()).isEqualTo(JobStatus.RUNNING);
            Assertions.assertThat(job2.jobStatus()).isEqualTo(JobStatus.RUNNING);
            Assertions.assertThat(job1.startedAt()).isNotNull();
            Assertions.assertThat(job2.startedAt()).isNotNull();
            Assertions.assertThat(job1.attemptCount()).isEqualTo(1);
            Assertions.assertThat(job2.attemptCount()).isEqualTo(1);
        }

        @Test
        void pickPending이_빈_리스트를_반환하면_빈_리스트를_반환한다() {
            // given
            Mockito.doReturn(List.of()).when(collectingJobRepository).pickPending(anyInt());

            // when
            List<UUID> result = collectingJobPicker.pickAndMarkRunning(10);

            // then
            Assertions.assertThat(result).isNotNull();
            Assertions.assertThat(result).isEmpty();
        }
    }
}
