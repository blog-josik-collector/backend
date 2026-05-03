package com.backend.integratedapi.collectingjob.service;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.common.enums.TriggerType;
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

        CollectingJob collectingJob = CollectingJob.builder()
                                                   .collectSource(collectSource)
                                                   .jobStatus(JobStatus.PENDING)
                                                   .triggerType(TriggerType.MANUAL)
                                                   .triggeredBy(dto.userId())
                                                   .build();

        CollectingJob savedCollectingJob = collectingJobRepository.save(collectingJob);

        return CollectingJobDto.from(savedCollectingJob);
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
