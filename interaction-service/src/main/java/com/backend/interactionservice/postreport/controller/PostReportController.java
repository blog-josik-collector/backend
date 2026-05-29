package com.backend.interactionservice.postreport.controller;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.interactionservice.postreport.controller.dto.PostReportCreateDto;
import com.backend.interactionservice.postreport.controller.dto.PostReportUpdateDto;
import com.backend.interactionservice.postreport.service.PostReportService;
import com.backend.interactionservice.postreport.service.dto.PostReportDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "05. 게시글 신고 API")
@RequestMapping("/interaction/v1")
@RestController
@RequiredArgsConstructor
public class PostReportController {

    private final PostReportService postReportService;

    /**
     * 1. 게시글 신고 등록. 인증된 사용자 누구나 신고할 수 있다.
     */
    @PostMapping("/postings/{postId}/reports")
    public ResponseEntity<PostReportCreateDto.Response> createReport(@PathVariable UUID postId,
                                                                     @RequestBody PostReportCreateDto.Request request,
                                                                     @AuthenticationPrincipal JwtPrincipal principal) {

        PostReportDto dto = postReportService.createReport(principal.getUserId(), postId, request.reportType(), request.content());
        return ResponseEntity.ok(PostReportCreateDto.Response.from(dto));
    }

    /**
     * 3. 게시글 신고 목록 조회 (관리자). 모든 필터는 선택 사항.
     * <p>
     * - status: 신고 처리 상태 (pending / resolved_deleted / rejected_keep)
     * <p>
     * - reportType: 신고 유형 (invalid_content / broken_link / other)
     * <p>
     * - start_date / end_date: created_at 기준 날짜 범위 (KST 기준 양 끝 inclusive). 형식: yyyy-MM-dd. 예: 2026-05-01
     */
    @GetMapping("/admin/reports/postings")
    public ResponseEntity<OffsetPageResult<PostReportDto>> getReports(@RequestParam(required = false) ReportStatus status,
                                                                      @RequestParam(name = "report_type", required = false) PostReportType reportType,
                                                                      @RequestParam(name = "start_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                                                                      @RequestParam(name = "end_date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                                                                      @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(postReportService.getReports(status, reportType, startDate, endDate, pageable));
    }

    /**
     * 4. 게시글 신고 상태 변경 (관리자). PENDING 신고를 RESOLVED_DELETED 또는 REJECTED_KEEP 으로만 변경 가능.
     */
    @PatchMapping("/admin/reports/postings/{reportId}")
    public ResponseEntity<PostReportUpdateDto.Response> changeStatus(@PathVariable UUID reportId,
                                                                     @RequestBody PostReportUpdateDto.Request request) {

        PostReportDto dto = postReportService.changeStatus(reportId, request.status());
        return ResponseEntity.ok(PostReportUpdateDto.Response.from(dto));
    }
}
