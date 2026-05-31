package com.backend.interactionservice.post.repository;

import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.post.QPost;
import com.backend.commondataaccess.persistence.post.QPostBookmark;
import com.backend.commondataaccess.persistence.post.QPostLike;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
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
     * soft-delete 되지 않은 posts 를 id 오름차순으로 batch 조회한다. lastId 가 null 이면 처음부터, 아니면 lastId 보다 큰 id 부터 limit 개를 반환한다. ES 카운트 동기화 worker 의 full-scan pagination 에 사용한다.
     */
    public List<Post> fetchActivePostsAfterId(UUID lastId, int limit) {
        BooleanBuilder where = new BooleanBuilder()
                .and(post.deletedAt.isNull());

        if (lastId != null) {
            where.and(post.id.gt(lastId));
        }

        return queryFactory.selectFrom(post)
                           .where(where)
                           .orderBy(post.id.asc())
                           .limit(limit)
                           .fetch();
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

    /**
     * posts.comment_count를 1 증가시킨다. 댓글/대댓글 모두 포함한다.
     */
    public long incrementCommentCount(UUID postId) {
        return queryFactory.update(post)
                           .set(post.commentCount, post.commentCount.add(1))
                           .where(post.id.eq(postId),
                                  post.deletedAt.isNull())
                           .execute();
    }

    /**
     * posts.comment_count를 1 감소시킨다. 음수로 떨어지지 않도록 comment_count > 0 조건을 함께 건다.
     */
    public long decrementCommentCount(UUID postId) {
        return queryFactory.update(post)
                           .set(post.commentCount, post.commentCount.subtract(1))
                           .where(post.id.eq(postId),
                                  post.deletedAt.isNull(),
                                  post.commentCount.gt(0))
                           .execute();
    }

    /**
     * posts.total_report_count를 1 증가시킨다. "누적 신고 건수" 의미로 createReport 시점에만 호출되며, 이후 신고 상태 변경에는 영향을 받지 않는다.
     */
    public long incrementTotalReportCount(UUID postId) {
        return queryFactory.update(post)
                           .set(post.totalReportCount, post.totalReportCount.add(1))
                           .where(post.id.eq(postId),
                                  post.deletedAt.isNull())
                           .execute();
    }

    /**
     * 여러 post의 view_count를 한 번에 증가시킨다. 각 entry의 value 만큼 atomic하게 add 한다. view 트래킹은 Redis에서 누적된 후 주기적으로 이 메서드로 flush 된다.
     */
    public void applyViewCounts(Map<UUID, Long> increments) {
        for (Map.Entry<UUID, Long> e : increments.entrySet()) {
            long delta = e.getValue();
            if (delta <= 0) {
                continue;
            }
            queryFactory.update(post)
                        .set(post.viewCount, post.viewCount.add((int) delta))
                        .where(post.id.eq(e.getKey()),
                               post.deletedAt.isNull())
                        .execute();
        }
    }
}
