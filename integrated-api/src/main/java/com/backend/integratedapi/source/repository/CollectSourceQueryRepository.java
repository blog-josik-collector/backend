package com.backend.integratedapi.source.repository;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.source.CollectSource;
import com.backend.commondataaccess.persistence.source.QCollectSource;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectSourceQueryRepository {

    private final JPAQueryFactory queryFactory;

    QCollectSource collectSource = QCollectSource.collectSource;

    public OffsetPageResult<CollectSource> fetchCollectSources(int page, int size) {
        List<CollectSource> collectSources = queryFactory
                .selectFrom(collectSource)
                .offset(page)
                .limit(size)
                .fetch();

        Long totalCount = queryFactory.select(collectSource.count()).from(collectSource).fetchOne();

        return new OffsetPageResult<>(totalCount != null ? totalCount : 0L,
                                      page,
                                      size,
                                      collectSources);
    }

    public Optional<CollectSource> fetchOneById(UUID id) {
        CollectSource result = queryFactory.select(collectSource)
                                           .from(collectSource)
                                           .where(
                                                   collectSource.id.eq(id),
                                                   collectSource.deletedAt.isNull()
                                           )
                                           .fetchOne();

        return Optional.ofNullable(result);
    }
}
