package com.backend.integratedworker.indexingjob.repository;

import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.commondataaccess.persistence.indexingjob.QIndexingJob;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class IndexingJobQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QIndexingJob indexingJob = QIndexingJob.indexingJob;

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
