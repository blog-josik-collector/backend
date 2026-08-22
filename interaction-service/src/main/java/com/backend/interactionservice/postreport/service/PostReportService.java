package com.backend.interactionservice.postreport.service;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.report.PostReport;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.service.PostService;
import com.backend.interactionservice.postreport.repository.PostReportQueryRepository;
import com.backend.interactionservice.postreport.repository.PostReportRepository;
import com.backend.interactionservice.postreport.service.dto.PostReportDto;
import com.backend.interactionservice.postreport.service.validator.PostReportValidator;
import com.backend.interactionservice.user.service.UserService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class PostReportService {

    private static final ZoneOffset KST = ZoneOffset.of("+09:00");
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

    private final PostReportRepository postReportRepository;
    private final PostReportQueryRepository queryRepository;
    private final PostQueryRepository postQueryRepository;
    private final PostService postService;
    private final UserService userService;

    /**
     * 1. 게시글 신고 등록. 등록 시점의 상태는 항상 PENDING 이다.
     * <p>
     * 동일 사용자가 동일 게시글에 대해 PENDING 상태인 활성 신고가 이미 있으면 중복 신고를 허용하지 않는다.
     */
    public PostReportDto createReport(UUID userId, UUID postId, PostReportType reportType, String content) {
        PostReportValidator.validateUserId(userId);
        PostReportValidator.validatePostId(postId);
        PostReportValidator.validateReportType(reportType);
        PostReportValidator.validateContent(content);
        PostReportValidator.validateNoPendingReport(queryRepository.existsPendingByUserIdAndPostId(userId, postId));

        User user = userService.getUser(userId);
        Post post = postService.getPost(postId);

        PostReport report = PostReport.builder()
                                      .user(user)
                                      .post(post)
                                      .reportStatus(ReportStatus.PENDING)
                                      .postReportType(reportType)
                                      .content(content)
                                      .build();

        PostReport saved = postReportRepository.saveAndFlush(report);
        postQueryRepository.incrementTotalReportCount(postId);
        return PostReportDto.from(saved);
    }

    /**
     * 3. 게시글 신고 목록 조회. 모든 필터(status, reportType, startDate, endDate)는 선택 사항이며 null 이면 해당 조건은 무시된다.
     * <p>
     * startDate / endDate 는 KST 기준 날짜 단위 range 로, 내부적으로 startDate 는 해당 일의 00:00:00+09:00, endDate 는 해당 일의 23:59:59+09:00 으로 변환되어 created_at 과 비교된다 (양 끝 inclusive).
     */
    @Transactional(readOnly = true)
    public OffsetPageResult<PostReportDto> getReports(ReportStatus status,
                                                      PostReportType reportType,
                                                      LocalDate startDate,
                                                      LocalDate endDate,
                                                      Pageable pageable) {

        OffsetDateTime startDateTime = startDate == null ? null : startDate.atStartOfDay().atOffset(KST);
        OffsetDateTime endDateTime = endDate == null ? null : endDate.atTime(END_OF_DAY).atOffset(KST);

        return queryRepository.fetchPage(status, reportType, startDateTime, endDateTime, pageable)
                              .map(PostReportDto::from);
    }

    /**
     * 4. 게시글 신고 상태 변경. PENDING 신고만 변경할 수 있고, 다시 PENDING 으로 되돌릴 수는 없다.
     * RESOLVED_DELETED 로 처리하면 신고 대상 게시글의 PostStatus 도 DELETED 로 변경한다.
     */
    public PostReportDto changeStatus(UUID reportId, ReportStatus newStatus) {
        PostReportValidator.validateNewStatus(newStatus);

        PostReport report = PostReportValidator.getPostReportOrThrow(reportId, queryRepository::fetchOneById);
        report.changeStatus(newStatus);

        if (newStatus == ReportStatus.RESOLVED_DELETED) {
            Post post = report.post();
            if (post.postStatus() != PostStatus.DELETED) {
                post.markDeleted();
            }
        }

        return PostReportDto.from(report);
    }
}
