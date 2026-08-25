package com.backend.interactionservice.postlike.repository;

import com.backend.commondataaccess.persistence.post.PostLike;
import com.backend.commondataaccess.persistence.post.QPostLike;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
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

    /**
     * 특정 유저의 soft-delete 되지 않은 좋아요 전체 조회. 회원 탈퇴 시 bulk soft-delete 용.
     */
    public List<PostLike> fetchAllActiveByUserId(UUID userId) {
        return queryFactory.selectFrom(postLike)
                           .leftJoin(postLike.post).fetchJoin()
                           .where(postLike.user.id.eq(userId),
                                  postLike.deletedAt.isNull())
                           .fetch();
    }
}
