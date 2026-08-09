package com.backend.interactionservice.commentreport.controller;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.CommentReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.security.CurrentUser;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.interactionservice.commentreport.controller.dto.CommentReportCreateDto;
import com.backend.interactionservice.commentreport.controller.dto.CommentReportUpdateDto;
import com.backend.interactionservice.commentreport.service.CommentReportService;
import com.backend.interactionservice.commentreport.service.dto.CommentReportDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "06. 댓글 신고 API")
@RequestMapping(value = "/interaction/v1", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequiredArgsConstructor
public class CommentReportController {

    private final CommentReportService commentReportService;

    /**
     * 2. 댓글 신고 등록. 인증된 사용자 누구나 신고할 수 있다.
     */
    @Operation(summary = "댓글 신고 등록")
    @PostMapping(value = "/comments/{commentId}/reports", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommentReportCreateDto.Response> createReport(@PathVariable UUID commentId,
                                                                        @RequestBody CommentReportCreateDto.Request request,
                                                                        @CurrentUser JwtPrincipal principal) {

        CommentReportDto dto = commentReportService.createReport(principal.getUserId(), commentId, request.reportType(), request.content());
        return ResponseEntity.ok(CommentReportCreateDto.Response.from(dto));
    }

    /**
     * 5. 댓글 신고 목록 조회 (관리자). 모든 필터는 선택 사항.
     * <p>
     * - status: 신고 처리 상태 (pending / resolved_deleted / rejected_keep)
     * <p>
     * - reportType: 신고 유형 (political / adult / other)
     * <p>
     * - start_date / end_date: created_at 기준 날짜 범위 (KST 기준 양 끝 inclusive). 형식: yyyy-MM-dd. 예: 2026-05-01
     */
    @Operation(summary = "댓글 신고 목록 조회")
    @GetMapping("/admin/reports/comments")
    public ResponseEntity<OffsetPageResult<CommentReportDto>> getReports(@RequestParam(required = false) ReportStatus status,
                                                                         @RequestParam(name = "report_type", required = false) CommentReportType reportType,
                                                                         @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                         @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                         @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(commentReportService.getReports(status, reportType, startDate, endDate, pageable));
    }

    /**
     * 6. 댓글 신고 상태 변경 (관리자). PENDING 신고를 RESOLVED_DELETED 또는 REJECTED_KEEP 으로만 변경 가능.
     */
    @Operation(summary = "댓글 신고 상태 변경")
    @PatchMapping(value = "/admin/reports/comments/{reportId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CommentReportUpdateDto.Response> changeStatus(@PathVariable UUID reportId,
                                                                        @RequestBody CommentReportUpdateDto.Request request) {

        CommentReportDto dto = commentReportService.changeStatus(reportId, request.status());
        return ResponseEntity.ok(CommentReportUpdateDto.Response.from(dto));
    }
}
