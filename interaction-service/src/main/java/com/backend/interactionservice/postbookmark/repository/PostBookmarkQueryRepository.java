package com.backend.interactionservice.postbookmark.repository;

import com.backend.commondataaccess.persistence.post.PostBookmark;
import com.backend.commondataaccess.persistence.post.QPostBookmark;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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
}
