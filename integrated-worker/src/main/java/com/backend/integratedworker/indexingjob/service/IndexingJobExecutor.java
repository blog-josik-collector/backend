package com.backend.integratedworker.indexingjob.service;

import com.backend.commondataaccess.exception.BusinessException;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedworker.indexingjob.service.dto.IndexingResult;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 비동기 실제 처리 오케스트레이터.
 * <p>
 * 옵션 B 설계: 자기 자신은 @Transactional을 갖지 않는다. 트랜잭션 경계는
 * 자식 서비스(IndexingService 하위의 PostService / CollectSourcePostService, IndexingJobService)가
 * 각자 짧은 트랜잭션으로 관리한다. 그렇게 해야 ES bulkIndex가 트랜잭션 밖에서 수행되고,
 * posts insert / collect_source_posts 마킹 / IndexingJob 상태 변경이 각각 별도로 커밋된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingJobExecutor {

    private final IndexingService indexingService;
    private final IndexingJobService indexingJobService;

    @Async("indexingExecutor")
    public void executeAsync(UUID jobId) {
        try {
            IndexingResult result = doIndexing(jobId);
            indexingJobService.updateCounts(jobId, result.totalCount(), result.indexedCount());
            indexingJobService.markSuccess(jobId, OffsetDateTime.now());
        } catch (Exception e) {
            if (e instanceof BusinessException businessException) {
                log.error("[IndexingJob][{}] job failed jobId={}",
                          businessException.getErrorCode().getCode(), jobId, e);
            } else {
                log.error("[IndexingJob][BE50001] job failed jobId={}", jobId, e);
            }
            indexingJobService.markFailed(jobId, OffsetDateTime.now(), e.getMessage());
        }
    }

    protected IndexingResult doIndexing(UUID jobId) {
        IndexingJob job = indexingJobService.getJob(jobId);
        return indexingService.executeIndexing(job);
    }
}
