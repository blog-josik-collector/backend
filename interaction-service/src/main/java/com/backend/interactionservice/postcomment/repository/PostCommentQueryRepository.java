package com.backend.interactionservice.postcomment.repository;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.PostCommentStatus;
import com.backend.commondataaccess.persistence.post.PostComment;
import com.backend.commondataaccess.persistence.post.QPostComment;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PostCommentQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QPostComment postComment = QPostComment.postComment;

    /**
     * 활성 상태(ACTIVE)이고 soft-delete 되지 않은 단건 조회. 작성자/포스팅 정보는 fetch join으로 함께 로드한다.
     */
    public Optional<PostComment> fetchOneById(UUID id) {
        PostComment result = queryFactory.selectFrom(postComment)
                                         .leftJoin(postComment.user).fetchJoin()
                                         .leftJoin(postComment.post).fetchJoin()
                                         .leftJoin(postComment.parentComment).fetchJoin()
                                         .where(postComment.id.eq(id),
                                                isActive())
                                         .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * 포스팅의 1-depth 댓글 목록 (parent_comment_id IS NULL) 페이지 조회.
     */
    public OffsetPageResult<PostComment> fetchCommentsByPostId(UUID postId, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder()
                .and(postComment.post.id.eq(postId))
                .and(postComment.parentComment.isNull())
                .and(isActive());

        return fetchPage(where, pageable);
    }

    /**
     * 특정 댓글의 대댓글 목록 (parent_comment_id = :parentCommentId) 페이지 조회.
     */
    public OffsetPageResult<PostComment> fetchRepliesByParentId(UUID parentCommentId, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder()
                .and(postComment.parentComment.id.eq(parentCommentId))
                .and(isActive());

        return fetchPage(where, pageable);
    }

    /**
     * 특정 유저가 작성한 댓글/대댓글 전체 페이지 조회. 댓글, 대댓글을 구분하지 않고 createdAt 내림차순으로 반환한다.
     */
    public OffsetPageResult<PostComment> fetchByUserId(UUID userId, Pageable pageable) {
        BooleanBuilder where = new BooleanBuilder()
                .and(postComment.user.id.eq(userId))
                .and(isActive());

        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();

        Long totalCount = queryFactory.select(postComment.count())
                                      .from(postComment)
                                      .where(where)
                                      .fetchOne();

        List<PostComment> contents = queryFactory.selectFrom(postComment)
                                                 .leftJoin(postComment.user).fetchJoin()
                                                 .leftJoin(postComment.post).fetchJoin()
                                                 .leftJoin(postComment.parentComment).fetchJoin()
                                                 .where(where)
                                                 .orderBy(postComment.createdAt.desc())
                                                 .offset(offset)
                                                 .limit(size)
                                                 .fetch();

        return new OffsetPageResult<>(totalCount == null ? 0L : totalCount, offset, size, contents);
    }

    /**
     * 주어진 parentIds 중에서 활성 상태인 자식 댓글을 1개 이상 가진 parent의 id 집합을 반환한다. 페이지 결과에 has_child_comment 플래그를 일괄로 매핑할 때 사용한다.
     */
    public Set<UUID> findParentIdsHavingActiveChildren(Collection<UUID> parentIds) {
        if (parentIds == null || parentIds.isEmpty()) {
            return Set.of();
        }

        List<UUID> result = queryFactory.select(postComment.parentComment.id)
                                        .distinct()
                                        .from(postComment)
                                        .where(postComment.parentComment.id.in(parentIds),
                                               isActive())
                                        .fetch();

        return Set.copyOf(result);
    }

    private OffsetPageResult<PostComment> fetchPage(BooleanBuilder where, Pageable pageable) {
        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();

        Long totalCount = queryFactory.select(postComment.count())
                                      .from(postComment)
                                      .where(where)
                                      .fetchOne();

        List<PostComment> contents = queryFactory.selectFrom(postComment)
                                                 .leftJoin(postComment.user).fetchJoin()
                                                 .leftJoin(postComment.post).fetchJoin()
                                                 .leftJoin(postComment.parentComment).fetchJoin()
                                                 .where(where)
                                                 .orderBy(postComment.createdAt.asc())
                                                 .offset(offset)
                                                 .limit(size)
                                                 .fetch();

        return new OffsetPageResult<>(totalCount == null ? 0L : totalCount, offset, size, contents);
    }

    private BooleanExpression isActive() {
        return postComment.deletedAt.isNull()
                                    .and(postComment.postCommentStatus.eq(PostCommentStatus.ACTIVE));
    }
}
