package com.backend.integratedapi.indexingjob.service;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedapi.collectsource.service.CollectSourceService;
import com.backend.integratedapi.collectsourcepost.service.CollectSourcePostService;
import com.backend.integratedapi.indexingjob.repository.IndexingJobQueryRepository;
import com.backend.integratedapi.indexingjob.repository.IndexingJobRepository;
import com.backend.integratedapi.indexingjob.service.dto.IndexingJobDto;
import com.backend.integratedapi.indexingjob.service.validator.IndexingJobValidator;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class IndexingJobService {

    private final IndexingJobRepository indexingJobRepository;
    private final IndexingJobQueryRepository queryRepository;
    private final CollectSourceService collectSourceService;
    private final CollectSourcePostService collectSourcePostService;

    public IndexingJob triggerReindexByCollectSource(UUID sourceId, UUID userId) {
        CollectSource source = collectSourceService.getCollectSource(sourceId);

        // 2) MANUAL IndexingJob을 PENDING으로 INSERT (워커가 픽업)
        IndexingJob job = IndexingJob.builder()
                                     .indexingJobType(IndexingJobType.MANUAL)
                                     .jobStatus(JobStatus.PENDING)
                                     .targetSource(source)
                                     .triggeredBy(userId)
                                     .build();

        return indexingJobRepository.save(job);
    }

    public IndexingJob triggerReindexByCollectSourcePost(UUID postId, UUID userId) {
        CollectSourcePost post = collectSourcePostService.getCollectSourcePost(postId);

        // 1) 이미 진행 중인 MANUAL post 재색인 Job이 있으면 거부
        if (queryRepository.existsActiveManualJobForPost(postId)) {
            throw new IllegalStateException("이미 진행 중인 재색인 작업이 있음: postId=" + postId);
        }

        // 2) MANUAL IndexingJob 생성 (PENDING, target_post_id 채움, target_source_id는 null)
        IndexingJob job = IndexingJob.builder()
                                     .indexingJobType(IndexingJobType.MANUAL)
                                     .jobStatus(JobStatus.PENDING)
                                     .targetPost(post)
                                     .triggeredBy(userId)
                                     .build();

        return indexingJobRepository.save(job);
    }

    @Transactional(readOnly = true)
    public OffsetPageResult<IndexingJobDto> getIndexingJobs(int page, int size) {
        return queryRepository.fetchIndexingJobs(page, size)
                              .map(IndexingJobDto::from);
    }

    @Transactional(readOnly = true)
    public IndexingJobDto getIndexingJobDto(UUID id) {
        IndexingJobValidator.validateId(id);
        IndexingJob indexingJob = getIndexingJob(id);
        return IndexingJobDto.from(indexingJob);
    }

    @Transactional(readOnly = true)
    public IndexingJob getIndexingJob(UUID id) {
        IndexingJobValidator.validateId(id);
        return IndexingJobValidator.getIndexingJobOrThrow(id, queryRepository::fetchOneById);
    }
}
