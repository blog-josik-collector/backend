package com.backend.interactionservice.postreport.service.dto;

import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.report.PostReport;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PostReportDto(UUID id,
                            UUID postId,
                            UUID reporterId,
                            ReportStatus status,
                            PostReportType reportType,
                            String content,
                            OffsetDateTime createdAt,
                            OffsetDateTime updatedAt) {

    public static PostReportDto from(PostReport report) {
        return new PostReportDto(
                report.id(),
                report.post() != null ? report.post().id() : null,
                report.user() != null ? report.user().id() : null,
                report.reportStatus(),
                report.postReportType(),
                report.content(),
                report.createdAt(),
                report.updatedAt()
        );
    }
}
