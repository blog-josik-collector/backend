package com.backend.integratedapi.indexingjob.repository;

import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.commondataaccess.persistence.indexingjob.QIndexingJob;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IndexingJobQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QIndexingJob indexingJob = QIndexingJob.indexingJob;

    public boolean existsActiveManualJobForPost(UUID postId) {
        List<IndexingJob> result = queryFactory.select(indexingJob)
                                               .from(indexingJob)
                                               .where(
                                                 indexingJob.targetPost.id.eq(postId),
                                                 indexingJob.indexingJobType.eq(IndexingJobType.MANUAL),
                                                 indexingJob.jobStatus.in(JobStatus.PENDING, JobStatus.RUNNING),
                                                 indexingJob.deletedAt.isNull()
                                         )
                                               .fetch();

        return !result.isEmpty();
    }

}
