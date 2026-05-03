package com.backend.integratedworker.collectingjob.repository;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectingjob.QCollectingJob;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectingJobQueryRepository {

    private final JPAQueryFactory queryFactory;

    QCollectingJob collectingJob = QCollectingJob.collectingJob;

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
}
