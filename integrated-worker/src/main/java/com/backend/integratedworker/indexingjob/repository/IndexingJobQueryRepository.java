package com.backend.integratedworker.indexingjob.repository;

import com.backend.commondataaccess.persistence.collectsource.QCollectSource;
import com.backend.commondataaccess.persistence.collectsource.QCollectSourcePost;
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
    private final QCollectSource collectSource = QCollectSource.collectSource;
    private final QCollectSourcePost collectSourcePost = QCollectSourcePost.collectSourcePost;

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

    /**
     * MANUAL 재색인 실행에 필요한 targetSource/targetPost 를 fetch join 으로 함께 조회한다.
     * 반환 시점에 트랜잭션이 닫히므로 detached 상태로 넘어간다.
     */
    public Optional<IndexingJob> fetchOneWithTargetsById(UUID id) {
        IndexingJob result = queryFactory.selectFrom(indexingJob)
                                         .leftJoin(indexingJob.targetSource, collectSource).fetchJoin()
                                         .leftJoin(indexingJob.targetPost, collectSourcePost).fetchJoin()
                                         .where(
                                                 indexingJob.id.eq(id),
                                                 indexingJob.deletedAt.isNull()
                                         )
                                         .fetchOne();

        return Optional.ofNullable(result);
    }
}
