package com.backend.interactionservice.postbookmark.repository;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.post.PostBookmark;
import com.backend.commondataaccess.persistence.post.QPostBookmark;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostBookmarkQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QPostBookmark postBookmark = QPostBookmark.postBookmark;

    public Optional<PostBookmark> fetchOneByUserAndPost(UUID userId, UUID postId) {
        PostBookmark result = queryFactory.selectFrom(postBookmark)
                                          .where(postBookmark.user.id.eq(userId),
                                                 postBookmark.post.id.eq(postId),
                                                 postBookmark.deletedAt.isNull())
                                          .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * 사용자가 활성 상태로 즐겨찾기한 post_id 목록을 createdAt 내림차순으로 페이지 조회한다.
     */
    public OffsetPageResult<UUID> fetchActivePostIdsByUserId(UUID userId, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder()
                .and(postBookmark.user.id.eq(userId))
                .and(postBookmark.isEnable.isTrue())
                .and(postBookmark.deletedAt.isNull());

        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();

        Long totalCount = queryFactory.select(postBookmark.count())
                                      .from(postBookmark)
                                      .where(where)
                                      .fetchOne();

        List<UUID> postIds = queryFactory.select(postBookmark.post.id)
                                         .from(postBookmark)
                                         .where(where)
                                         .orderBy(postBookmark.createdAt.desc())
                                         .offset(offset)
                                         .limit(size)
                                         .fetch();

        return new OffsetPageResult<>(totalCount == null ? 0L : totalCount, offset, size, postIds);
    }
}
