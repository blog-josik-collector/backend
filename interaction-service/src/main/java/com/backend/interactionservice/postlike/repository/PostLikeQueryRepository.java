package com.backend.interactionservice.postlike.repository;

import com.backend.commondataaccess.persistence.post.PostLike;
import com.backend.commondataaccess.persistence.post.QPostLike;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostLikeQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QPostLike postLike = QPostLike.postLike;

    public Optional<PostLike> fetchOneByUserAndPost(UUID userId, UUID postId) {
        PostLike result = queryFactory.selectFrom(postLike)
                                      .where(postLike.user.id.eq(userId),
                                             postLike.post.id.eq(postId),
                                             postLike.deletedAt.isNull())
                                      .fetchOne();

        return Optional.ofNullable(result);
    }
}
