package com.backend.interactionservice.commentreport.service.dto;

import com.backend.commondataaccess.persistence.common.enums.CommentReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.persistence.report.CommentReport;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "댓글 신고 내역")
public record CommentReportDto(
        @Schema(description = "신고 ID")
        UUID id,

        @Schema(description = "신고 대상 댓글 본문")
        String commentContent,

        @Schema(description = "신고자 닉네임")
        String nickname,

        @Schema(description = "처리 상태")
        ReportStatus status,

        @Schema(description = "신고 유형")
        CommentReportType reportType,

        @Schema(description = "신고 상세 사유")
        String content,

        @Schema(description = "신고 접수 시각")
        OffsetDateTime createdAt,

        @Schema(description = "최종 변경 시각")
        OffsetDateTime updatedAt) {

    public static CommentReportDto from(CommentReport report) {
        return new CommentReportDto(
                report.id(),
                report.comment() != null ? report.comment().content() : null,
                report.user() != null ? report.user().nickname() : null,
                report.reportStatus(),
                report.commentReportType(),
                report.content(),
                report.createdAt(),
                report.updatedAt()
        );
    }
}
