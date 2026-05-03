package com.backend.integratedworker.collectsource.repository;

import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.QCollectSource;
import com.backend.commondataaccess.persistence.common.enums.ScheduleType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectSourceQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QCollectSource collectSource = QCollectSource.collectSource;

    public List<CollectSource> findActiveCronCollectSources() {
        return queryFactory.select(collectSource)
                           .from(collectSource)
                           .where(
                                   collectSource.scheduleType.eq(ScheduleType.CRON),
                                   collectSource.isUsed.eq(true),
                                   collectSource.deletedAt.isNull()
                           )
                           .fetch();
    }
}
