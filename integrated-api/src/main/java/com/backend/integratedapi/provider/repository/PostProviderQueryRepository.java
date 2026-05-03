package com.backend.integratedapi.provider.repository;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.commondataaccess.persistence.provider.QPostProvider;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostProviderQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QPostProvider postProvider = QPostProvider.postProvider;

    public OffsetPageResult<PostProvider> fetchPostProviders(int page, int size) {
        List<PostProvider> postProviders = queryFactory
                .selectFrom(postProvider)
                .where(postProvider.deletedAt.isNull())
                .offset(page)
                .limit(size)
                .fetch();

        Long totalCount = queryFactory.select(postProvider.count())
                                      .from(postProvider)
                                      .where(postProvider.deletedAt.isNull())
                                      .fetchOne();

        return new OffsetPageResult<>(totalCount != null ? totalCount : 0L,
                                      page,
                                      size,
                                      postProviders);
    }

    public Optional<PostProvider> fetchOneById(UUID id) {
        PostProvider result = queryFactory.select(postProvider)
                                          .from(postProvider)
                                          .where(
                                                  postProvider.id.eq(id),
                                                  postProvider.deletedAt.isNull()
                                          )
                                          .fetchOne();

        return Optional.ofNullable(result);
    }


}
