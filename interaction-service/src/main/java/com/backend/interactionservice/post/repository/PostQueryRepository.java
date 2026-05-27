package com.backend.interactionservice.post.repository;

import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.post.QPost;
import com.backend.commondataaccess.persistence.post.QPostBookmark;
import com.backend.commondataaccess.persistence.post.QPostLike;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QPost post = QPost.post;
    private final QPostLike postLike = QPostLike.postLike;
    private final QPostBookmark postBookmark = QPostBookmark.postBookmark;

    /**
     * 주어진 postIds 중에서 userId가 현재 활성 상태로 좋아요를 누른 post의 id 집합. userId가 null이거나 postIds가 비어있으면 빈 Set을 반환한다.
     */
    public Optional<Post> fetchOneById(UUID id) {
        Post result = queryFactory.selectFrom(post)
                                  .where(post.id.eq(id),
                                         post.deletedAt.isNull()
                                  )
                                  .fetchOne();

        return Optional.ofNullable(result);
    }

    public Set<UUID> findLikedPostIds(UUID userId, Collection<UUID> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(queryFactory.select(postLike.post.id)
                                      .from(postLike)
                                      .where(postLike.user.id.eq(userId),
                                             postLike.post.id.in(postIds),
                                             postLike.isEnable.isTrue(),
                                             postLike.deletedAt.isNull())
                                      .fetch());
    }

    /**
     * 주어진 postIds 중에서 userId가 현재 활성 상태로 북마크를 누른 post의 id 집합. userId가 null이거나 postIds가 비어있으면 빈 Set을 반환한다.
     */
    public Set<UUID> findBookmarkedPostIds(UUID userId, Collection<UUID> postIds) {
        if (userId == null || postIds == null || postIds.isEmpty()) {
            return Set.of();
        }

        return Set.copyOf(queryFactory.select(postBookmark.post.id)
                                      .from(postBookmark)
                                      .where(postBookmark.user.id.eq(userId),
                                             postBookmark.post.id.in(postIds),
                                             postBookmark.isEnable.isTrue(),
                                             postBookmark.deletedAt.isNull())
                                      .fetch());
    }

    /**
     * posts.like_count를 1 증가시킨다. 동시 요청의 lost-update를 막기 위해 DB에서 atomic하게 처리한다.
     */
    public long incrementLikeCount(UUID postId) {
        return queryFactory.update(post)
                           .set(post.likeCount, post.likeCount.add(1))
                           .where(post.id.eq(postId),
                                  post.deletedAt.isNull())
                           .execute();
    }

    /**
     * posts.like_count를 1 감소시킨다. 음수로 떨어지지 않도록 like_count > 0 조건을 함께 건다.
     */
    public long decrementLikeCount(UUID postId) {
        return queryFactory.update(post)
                           .set(post.likeCount, post.likeCount.subtract(1))
                           .where(post.id.eq(postId),
                                  post.deletedAt.isNull(),
                                  post.likeCount.gt(0))
                           .execute();
    }
}
