package com.backend.interactionservice.postreport.service.validator;

import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.report.PostReport;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PostReportValidator {

    private static final int MAX_CONTENT_LENGTH = 1000;

    public static void validateUserId(UUID userId) {
        if (ObjectUtils.isEmpty(userId)) {
            throw new IllegalArgumentException("userId는 필수 입력값입니다.");
        }
    }

    public static void validatePostId(UUID postId) {
        if (ObjectUtils.isEmpty(postId)) {
            throw new IllegalArgumentException("postId는 필수 입력값입니다.");
        }
    }

    public static void validateReportId(UUID reportId) {
        if (ObjectUtils.isEmpty(reportId)) {
            throw new IllegalArgumentException("reportId는 필수 입력값입니다.");
        }
    }

    public static void validateReportType(PostReportType reportType) {
        if (reportType == null) {
            throw new IllegalArgumentException("신고 유형(reportType)은 필수 입력값입니다.");
        }
    }

    public static void validateContent(String content) {
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("신고 사유는 비어있을 수 없습니다.");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException(String.format("신고 사유는 %d자를 초과할 수 없습니다.", MAX_CONTENT_LENGTH));
        }
    }

    public static void validateNewStatus(ReportStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("변경할 신고 상태는 필수 입력값입니다.");
        }
        if (newStatus == ReportStatus.PENDING) {
            throw new IllegalArgumentException("신고를 PENDING 으로 되돌릴 수 없습니다.");
        }
    }

    public static PostReport getPostReportOrThrow(UUID reportId, Function<UUID, Optional<PostReport>> fetchOneById) {
        validateReportId(reportId);

        return fetchOneById.apply(reportId)
                           .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글 신고입니다. id: " + reportId));
    }
}
