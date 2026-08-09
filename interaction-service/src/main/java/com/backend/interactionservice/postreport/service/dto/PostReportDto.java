package com.backend.interactionservice.postreport.service.dto;

import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.report.PostReport;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "게시글 신고 내역")
public record PostReportDto(
        @Schema(description = "신고 ID")
        UUID id,

        @Schema(description = "신고 대상 게시글 ID")
        UUID postId,

        @Schema(description = "신고자 ID")
        UUID reporterId,

        @Schema(description = "처리 상태")
        ReportStatus status,

        @Schema(description = "신고 유형")
        PostReportType reportType,

        @Schema(description = "신고 상세 사유")
        String content,

        @Schema(description = "신고 접수 시각")
        OffsetDateTime createdAt,

        @Schema(description = "최종 변경 시각")
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
