package com.backend.integratedapi.indexingjob.repository;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.commondataaccess.persistence.indexingjob.QIndexingJob;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
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

    public OffsetPageResult<IndexingJob> fetchIndexingJobs(int page, int size) {
        List<IndexingJob> collectSources = queryFactory
                .selectFrom(indexingJob)
                .where(indexingJob.deletedAt.isNull())
                .offset(page)
                .limit(size)
                .fetch();

        Long totalCount = queryFactory.select(indexingJob.count())
                                      .from(indexingJob)
                                      .where(indexingJob.deletedAt.isNull())
                                      .fetchOne();

        return new OffsetPageResult<>(totalCount != null ? totalCount : 0L,
                                      page,
                                      size,
                                      collectSources);
    }

    public Optional<IndexingJob> fetchOneById(UUID id) {
        IndexingJob result = queryFactory.select(indexingJob)
                                         .from(indexingJob)
                                         .where(
                                                 indexingJob.id.eq(id),
                                                 indexingJob.deletedAt.isNull()
                                         )
                                         .fetchOne();

        return Optional.ofNullable(result);
    }
}
