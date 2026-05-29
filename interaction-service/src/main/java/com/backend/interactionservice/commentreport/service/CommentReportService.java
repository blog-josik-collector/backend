package com.backend.interactionservice.commentreport.service;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.CommentReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.post.PostComment;
import com.backend.commondataaccess.persistence.report.CommentReport;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.interactionservice.commentreport.repository.CommentReportQueryRepository;
import com.backend.interactionservice.commentreport.repository.CommentReportRepository;
import com.backend.interactionservice.commentreport.service.dto.CommentReportDto;
import com.backend.interactionservice.commentreport.service.validator.CommentReportValidator;
import com.backend.interactionservice.postcomment.service.PostCommentService;
import com.backend.interactionservice.user.service.UserService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class CommentReportService {

    private static final ZoneOffset KST = ZoneOffset.of("+09:00");
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

    private final CommentReportRepository commentReportRepository;
    private final CommentReportQueryRepository queryRepository;
    private final PostCommentService postCommentService;
    private final UserService userService;

    /**
     * 2. 댓글 신고 등록. 등록 시점의 상태는 항상 PENDING 이다.
     * <p>
     * 동일 사용자가 동일 댓글에 대해 활성 상태(deleted_at IS NULL)인 신고를 이미 가지고 있으면 부분 unique index에 의해 INSERT가 실패한다. 이 경우 명시적으로 "이미 신고한 댓글입니다." 예외로 변환한다.
     */
    public CommentReportDto createReport(UUID userId, UUID commentId, CommentReportType reportType, String content) {
        CommentReportValidator.validateUserId(userId);
        CommentReportValidator.validateCommentId(commentId);
        CommentReportValidator.validateReportType(reportType);
        CommentReportValidator.validateContent(content);

        User user = userService.getUser(userId);
        PostComment comment = postCommentService.getComment(commentId);

        CommentReport report = CommentReport.builder()
                                            .user(user)
                                            .comment(comment)
                                            .reportStatus(ReportStatus.PENDING)
                                            .commentReportType(reportType)
                                            .content(content)
                                            .build();

        try {
            CommentReport saved = commentReportRepository.saveAndFlush(report);
            return CommentReportDto.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("이미 신고한 댓글입니다.");
        }
    }

    /**
     * 5. 댓글 신고 목록 조회. 모든 필터(status, reportType, startDate, endDate)는 선택 사항이며 null 이면 해당 조건은 무시된다.
     * <p>
     * startDate / endDate 는 KST 기준 날짜 단위 range 로, 내부적으로 startDate 는 해당 일의 00:00:00+09:00, endDate 는 해당 일의 23:59:59+09:00 으로 변환되어 created_at 과 비교된다 (양 끝 inclusive).
     */
    @Transactional(readOnly = true)
    public OffsetPageResult<CommentReportDto> getReports(ReportStatus status,
                                                         CommentReportType reportType,
                                                         LocalDate startDate,
                                                         LocalDate endDate,
                                                         Pageable pageable) {

        OffsetDateTime startDateTime = startDate == null ? null : startDate.atStartOfDay().atOffset(KST);
        OffsetDateTime endDateTime = endDate == null ? null : endDate.atTime(END_OF_DAY).atOffset(KST);

        return queryRepository.fetchPage(status, reportType, startDateTime, endDateTime, pageable)
                              .map(CommentReportDto::from);
    }

    /**
     * 6. 댓글 신고 상태 변경. PENDING 신고만 변경할 수 있고, 다시 PENDING 으로 되돌릴 수는 없다.
     */
    public CommentReportDto changeStatus(UUID reportId, ReportStatus newStatus) {
        CommentReportValidator.validateNewStatus(newStatus);

        CommentReport report = CommentReportValidator.getCommentReportOrThrow(reportId, queryRepository::fetchOneById);
        report.changeStatus(newStatus);

        return CommentReportDto.from(report);
    }
}
