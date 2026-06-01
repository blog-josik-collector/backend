package com.backend.integratedapi.collectingjob.service;

import static com.backend.commondataaccess.persistence.common.enums.CollectScheduleType.CRON;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.exception.StateConflictException;
import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.integratedapi.collectingjob.repository.CollectingJobQueryRepository;
import com.backend.integratedapi.collectingjob.repository.CollectingJobRepository;
import com.backend.integratedapi.collectingjob.service.dto.CollectingJobDto;
import com.backend.integratedapi.collectingjob.service.validator.CollectingJobValidator;
import com.backend.integratedapi.collectsource.service.CollectSourceService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class CollectingJobService {

    private final CollectingJobRepository collectingJobRepository;
    private final CollectingJobQueryRepository queryRepository;
    private final CollectSourceService collectSourceService;

    public CollectingJobDto start(CollectingJobDto dto) {
        CollectSource collectSource = collectSourceService.getCollectSource(dto.sourceId());

        return switch (collectSource.collectScheduleType()) {
            case CRON -> startCron(collectSource, dto);
            case MANUAL -> startManual(collectSource, dto);
        };
    }

    public void stop(UUID sourceId) {
        CollectSource collectSource = collectSourceService.getCollectSource(sourceId);

        if (collectSource.collectScheduleType().equals(CRON)) {
            collectSource.deactivate();
        }
    }

    private CollectingJobDto startCron(CollectSource source, CollectingJobDto dto) {
        source.activate();
        CollectingJob collectingJob = createPendingJob(source, dto);
        CollectingJob savedCollectingJob = collectingJobRepository.save(collectingJob);
        return CollectingJobDto.from(savedCollectingJob);
    }

    private CollectingJobDto startManual(CollectSource source, CollectingJobDto dto) {
        if (!source.isUsed()) {
            throw new StateConflictException("비활성 source는 실행할 수 없음");
        }
        if (queryRepository.existsActiveJob(source.id())) {
            throw new StateConflictException("이미 진행 중인 Job 있음");
        }
        CollectingJob collectingJob = createPendingJob(source, dto);
        CollectingJob savedCollectingJob = collectingJobRepository.save(collectingJob);
        return CollectingJobDto.from(savedCollectingJob);
    }

    private CollectingJob createPendingJob(CollectSource collectSource, CollectingJobDto dto) {
        return switch (collectSource.collectScheduleType()) {
            case CRON -> CollectingJob.builder()
                                      .collectSource(collectSource)
                                      .jobStatus(JobStatus.PENDING)
                                      .fromPage(dto.fromPage())
                                      .toPage(dto.toPage())
                                      .forceRecollect(dto.forceRecollect())
                                      .build();
            case MANUAL -> CollectingJob.builder()
                                        .collectSource(collectSource)
                                        .jobStatus(JobStatus.PENDING)
                                        .fromPage(dto.fromPage())
                                        .toPage(dto.toPage())
                                        .triggeredBy(dto.userId())
                                        .forceRecollect(dto.forceRecollect())
                                        .build();
        };
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<CollectingJobDto> getCollectingJobs(int page, int size) {
        return queryRepository.fetchCollectingJobs(page, size)
                              .map(CollectingJobDto::from);
    }

    @Transactional(readOnly = true)
    public CollectingJobDto getCollectingJobDto(UUID id) {
        CollectingJobValidator.validateId(id);
        CollectingJob collectingJob = getCollectingJob(id);
        return CollectingJobDto.from(collectingJob);
    }

    @Transactional(readOnly = true)
    public CollectingJob getCollectingJob(UUID id) {
        CollectingJobValidator.validateId(id);
        return CollectingJobValidator.getCollectingJobOrThrow(id, queryRepository::fetchOneById);
    }
}
