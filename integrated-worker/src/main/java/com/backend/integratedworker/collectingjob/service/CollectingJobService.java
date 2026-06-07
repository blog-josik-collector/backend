package com.backend.integratedworker.collectingjob.service;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.integratedworker.collectingjob.repository.CollectingJobQueryRepository;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import com.backend.integratedworker.collectsourcepost.service.CollectSourcePostService;
import com.backend.integratedworker.collectingjob.service.validator.CollectingJobValidator;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CollectingJob 엔티티의 상태 변경을 트랜잭션 단위로 처리한다.
 * <p>
 * CollectingJobExecutor는 @Async 스레드에서 크롤링(I/O)을 수행하므로 @Transactional을 갖지 않는다.
 * 크롤링 이후 DB persist + job SUCCESS 마킹은 {@link #finishCollect} 한 트랜잭션으로 처리한다.
 */
@Transactional
@Service
@RequiredArgsConstructor
public class CollectingJobService {

    private final CollectingJobQueryRepository queryRepository;
    private final CollectSourcePostService collectSourcePostService;

    /**
     * 크롤링 파라미터 조회용. collectSource·postProvider를 fetch join으로 로드한다.
     * 반환 시점에 트랜잭션이 닫히므로 detached 상태로 넘어간다.
     */
    @Transactional(readOnly = true)
    public CollectingJob getJobForExecution(UUID jobId) {
        return CollectingJobValidator.getCollectingJobOrThrow(jobId, queryRepository::fetchOneWithCollectSourceById);
    }

    public void updateCounts(UUID jobId, int totalCount, int collectedCount) {
        CollectingJob job = CollectingJobValidator.getCollectingJobOrThrow(jobId, queryRepository::fetchOneById);
        job.updateCounts(totalCount, collectedCount);
    }

    public void markSuccess(UUID jobId, OffsetDateTime now) {
        CollectingJob job = CollectingJobValidator.getCollectingJobOrThrow(jobId, queryRepository::fetchOneById);
        job.markSuccess(now);
    }

    /**
     * 카운트 갱신과 SUCCESS 마킹을 한 트랜잭션으로 처리한다.
     * updateCounts / markSuccess를 Executor에서 따로 호출하면 중간 커밋 후 markSuccess 실패 시
     * RUNNING + counts만 반영된 불일치 상태가 될 수 있다.
     */
    public void completeSuccess(UUID jobId, int totalCount, int collectedCount, OffsetDateTime now) {
        CollectingJob job = CollectingJobValidator.getCollectingJobOrThrow(jobId, queryRepository::fetchOneById);
        job.updateCounts(totalCount, collectedCount);
        job.markSuccess(now);
    }

    /**
     * 크롤링 결과 post persist + job SUCCESS 마킹을 한 트랜잭션으로 처리한다.
     */
    public void finishCollect(UUID jobId,
                              UUID collectSourceId,
                              boolean forceRecollect,
                              List<Post> posts,
                              OffsetDateTime now) {
        collectSourcePostService.persistCollectedPostsForJob(jobId, collectSourceId, forceRecollect, posts);
        completeSuccess(jobId, posts.size(), posts.size(), now);
    }

    public void markFailed(UUID jobId, OffsetDateTime now, String errorMessage) {
        CollectingJob job = CollectingJobValidator.getCollectingJobOrThrow(jobId, queryRepository::fetchOneById);
        job.markFailed(now, errorMessage);
    }
}
