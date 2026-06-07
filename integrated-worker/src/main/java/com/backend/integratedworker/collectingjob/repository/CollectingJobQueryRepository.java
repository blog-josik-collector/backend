package com.backend.integratedworker.collectingjob.repository;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectingjob.QCollectingJob;
import com.backend.commondataaccess.persistence.collectsource.QCollectSource;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.provider.QPostProvider;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectingJobQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QCollectingJob collectingJob = QCollectingJob.collectingJob;
    private final QCollectSource collectSource = QCollectSource.collectSource;
    private final QPostProvider postProvider = QPostProvider.postProvider;

    public Optional<CollectingJob> fetchOneById(UUID id) {
        CollectingJob result = queryFactory.select(collectingJob)
                                           .from(collectingJob)
                                           .where(
                                                   collectingJob.id.eq(id),
                                                   collectingJob.deletedAt.isNull()
                                           )
                                           .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * 크롤링에 필요한 collectSource·postProvider를 fetch join으로 함께 조회한다.
     * 반환 시점에 트랜잭션이 닫히므로 detached 상태로 넘어간다.
     */
    public Optional<CollectingJob> fetchOneWithCollectSourceById(UUID id) {
        CollectingJob result = queryFactory.selectFrom(collectingJob)
                                           .join(collectingJob.collectSource, collectSource).fetchJoin()
                                           .join(collectSource.postProvider, postProvider).fetchJoin()
                                           .where(
                                                   collectingJob.id.eq(id),
                                                   collectingJob.deletedAt.isNull()
                                           )
                                           .fetchOne();

        return Optional.ofNullable(result);
    }

    public boolean existsActiveJob(UUID collectSourceId) {
        List<CollectingJob> results = queryFactory.selectFrom(collectingJob)
                                                  .where(
                                                          collectingJob.collectSource.id.eq(collectSourceId),
                                                          collectingJob.jobStatus.in(JobStatus.PENDING, JobStatus.RUNNING),
                                                          collectingJob.deletedAt.isNull()
                                                  ).fetch();

        return !results.isEmpty();
    }
}
