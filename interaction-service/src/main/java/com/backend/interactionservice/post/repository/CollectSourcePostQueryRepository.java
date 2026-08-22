package com.backend.interactionservice.post.repository;

import com.backend.commondataaccess.persistence.collectsource.QCollectSourcePost;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectSourcePostQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QCollectSourcePost collectSourcePost = QCollectSourcePost.collectSourcePost;

    /**
     * posts.id(=collect_source_posts.id) 목록에 대응하는 title을 일괄 조회한다.
     */
    public Map<UUID, String> findTitlesByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        List<Tuple> rows = queryFactory.select(collectSourcePost.id, collectSourcePost.title)
                                       .from(collectSourcePost)
                                       .where(collectSourcePost.id.in(ids),
                                              collectSourcePost.deletedAt.isNull())
                                       .fetch();

        return rows.stream()
                   .collect(Collectors.toMap(row -> row.get(collectSourcePost.id),
                                             row -> row.get(collectSourcePost.title),
                                             (left, right) -> left));
    }
}
