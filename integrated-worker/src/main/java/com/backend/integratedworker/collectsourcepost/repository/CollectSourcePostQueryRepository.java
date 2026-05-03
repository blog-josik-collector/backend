package com.backend.integratedworker.collectsourcepost.repository;

import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.collectsource.QCollectSourcePost;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectSourcePostQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QCollectSourcePost collectSourcePost = QCollectSourcePost.collectSourcePost;

    public Optional<CollectSourcePost> fetchOneById(UUID id) {
        CollectSourcePost result = queryFactory.select(collectSourcePost)
                                               .from(collectSourcePost)
                                               .where(
                                                       collectSourcePost.id.eq(id),
                                                       collectSourcePost.deletedAt.isNull()
                                               )
                                               .fetchOne();

        return Optional.ofNullable(result);
    }

    public Optional<CollectSourcePost> fetchOneByUrl(String url) {
        CollectSourcePost result = queryFactory.select(collectSourcePost)
                                               .from(collectSourcePost)
                                               .where(
                                                       collectSourcePost.url.eq(url),
                                                       collectSourcePost.deletedAt.isNull()
                                               )
                                               .fetchOne();

        return Optional.ofNullable(result);
    }
}
