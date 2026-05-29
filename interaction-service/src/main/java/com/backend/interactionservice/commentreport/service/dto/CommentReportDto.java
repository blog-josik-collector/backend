package com.backend.interactionservice.commentreport.service.dto;

import com.backend.commondataaccess.persistence.common.enums.CommentReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.report.CommentReport;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CommentReportDto(UUID id,
                               UUID commentId,
                               UUID reporterId,
                               ReportStatus status,
                               CommentReportType reportType,
                               String content,
                               OffsetDateTime createdAt,
                               OffsetDateTime updatedAt) {

    public static CommentReportDto from(CommentReport report) {
        return new CommentReportDto(
                report.id(),
                report.comment() != null ? report.comment().id() : null,
                report.user() != null ? report.user().id() : null,
                report.reportStatus(),
                report.commentReportType(),
                report.content(),
                report.createdAt(),
                report.updatedAt()
        );
    }
}
