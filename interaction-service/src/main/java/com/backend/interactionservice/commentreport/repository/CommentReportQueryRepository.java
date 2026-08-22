package com.backend.interactionservice.commentreport.repository;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.CommentReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.report.CommentReport;
import com.backend.commondataaccess.persistence.report.QCommentReport;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CommentReportQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QCommentReport commentReport = QCommentReport.commentReport;

    /**
     * 동일 사용자가 동일 댓글에 대해 PENDING 상태인 활성 신고가 존재하는지 확인한다.
     */
    public boolean existsPendingByUserIdAndCommentId(UUID userId, UUID commentId) {
        Integer result = queryFactory.selectOne()
                                       .from(commentReport)
                                       .where(commentReport.user.id.eq(userId),
                                              commentReport.comment.id.eq(commentId),
                                              commentReport.reportStatus.eq(ReportStatus.PENDING),
                                              commentReport.deletedAt.isNull())
                                       .fetchFirst();
        return result != null;
    }

    /**
     * soft-delete 되지 않은 단건 조회. 신고자, 신고 대상 댓글까지 fetch join 한다.
     */
    public Optional<CommentReport> fetchOneById(UUID id) {
        CommentReport result = queryFactory.selectFrom(commentReport)
                                           .join(commentReport.user).fetchJoin()
                                           .join(commentReport.comment).fetchJoin()
                                           .where(commentReport.id.eq(id),
                                                  commentReport.deletedAt.isNull())
                                           .fetchOne();
        return Optional.ofNullable(result);
    }

    /**
     * 신고 목록을 페이지로 조회한다. 모든 필터 파라미터는 nullable 이며, null 인 필터는 조건에서 제외된다.
     * <p>
     * - status: 신고 처리 상태 일치 필터
     * <p>
     * - reportType: 신고 유형 일치 필터
     * <p>
     * - startDate / endDate: created_at 기준 양 끝 inclusive 범위 필터
     */
    public OffsetPageResult<CommentReport> fetchPage(ReportStatus status,
                                                     CommentReportType reportType,
                                                     OffsetDateTime startDate,
                                                     OffsetDateTime endDate,
                                                     Pageable pageable) {

        BooleanBuilder where = new BooleanBuilder()
                .and(commentReport.deletedAt.isNull());

        if (status != null) {
            where.and(commentReport.reportStatus.eq(status));
        }
        if (reportType != null) {
            where.and(commentReport.commentReportType.eq(reportType));
        }
        if (startDate != null) {
            where.and(commentReport.createdAt.goe(startDate));
        }
        if (endDate != null) {
            where.and(commentReport.createdAt.loe(endDate));
        }

        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();

        Long totalCount = queryFactory.select(commentReport.count())
                                      .from(commentReport)
                                      .where(where)
                                      .fetchOne();

        List<CommentReport> contents = queryFactory.selectFrom(commentReport)
                                                   .join(commentReport.user).fetchJoin()
                                                   .join(commentReport.comment).fetchJoin()
                                                   .where(where)
                                                   .orderBy(commentReport.createdAt.desc())
                                                   .offset(offset)
                                                   .limit(size)
                                                   .fetch();

        return new OffsetPageResult<>(totalCount == null ? 0L : totalCount, offset, size, contents);
    }
}
