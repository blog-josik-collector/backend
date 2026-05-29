package com.backend.interactionservice.postreport.repository;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.report.PostReport;
import com.backend.commondataaccess.persistence.report.QPostReport;
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
public class PostReportQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QPostReport postReport = QPostReport.postReport;

    /**
     * soft-delete 되지 않은 단건 조회. 신고자, 신고 대상 게시글까지 fetch join 한다.
     */
    public Optional<PostReport> fetchOneById(UUID id) {
        PostReport result = queryFactory.selectFrom(postReport)
                                        .join(postReport.user).fetchJoin()
                                        .join(postReport.post).fetchJoin()
                                        .where(postReport.id.eq(id),
                                               postReport.deletedAt.isNull())
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
    public OffsetPageResult<PostReport> fetchPage(ReportStatus status,
                                                  PostReportType reportType,
                                                  OffsetDateTime startDate,
                                                  OffsetDateTime endDate,
                                                  Pageable pageable) {

        BooleanBuilder where = new BooleanBuilder()
                .and(postReport.deletedAt.isNull());

        if (status != null) {
            where.and(postReport.reportStatus.eq(status));
        }
        if (reportType != null) {
            where.and(postReport.postReportType.eq(reportType));
        }
        if (startDate != null) {
            where.and(postReport.createdAt.goe(startDate));
        }
        if (endDate != null) {
            where.and(postReport.createdAt.loe(endDate));
        }

        int offset = (int) pageable.getOffset();
        int size = pageable.getPageSize();

        Long totalCount = queryFactory.select(postReport.count())
                                      .from(postReport)
                                      .where(where)
                                      .fetchOne();

        List<PostReport> contents = queryFactory.selectFrom(postReport)
                                                .join(postReport.user).fetchJoin()
                                                .join(postReport.post).fetchJoin()
                                                .where(where)
                                                .orderBy(postReport.createdAt.desc())
                                                .offset(offset)
                                                .limit(size)
                                                .fetch();

        return new OffsetPageResult<>(totalCount == null ? 0L : totalCount, offset, size, contents);
    }
}
