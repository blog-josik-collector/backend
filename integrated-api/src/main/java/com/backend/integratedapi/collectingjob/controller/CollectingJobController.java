package com.backend.integratedapi.collectingjob.controller;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.integratedapi.collectingjob.controller.dto.CollectingJobReadDto;
import com.backend.integratedapi.collectingjob.controller.dto.CollectingJobStartDto;
import com.backend.integratedapi.collectingjob.service.CollectingJobService;
import com.backend.integratedapi.collectingjob.service.dto.CollectingJobDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "02. 수집 작업 실행 API")
@RequestMapping(value = "/collect/v1")
@RestController
@RequiredArgsConstructor
public class CollectingJobController {

    private final CollectingJobService collectingJobService;

    @Value("${crawler.default-from-page}")
    private String defaultFromPage;

    @Value("${crawler.default-to-page}")
    private String defaultToPage;

    @Operation(
            summary = "수집 작업 시작",
            description = "MANUAL source는 1회 Job 생성 후 종료. CRON source는 자동 생성 사이클 활성화 + 첫 Job 생성."
    )
    @PostMapping("/sources/{source-id}/_start")
    public ResponseEntity<CollectingJobStartDto.Response> start(@AuthenticationPrincipal JwtPrincipal authentication,
                                                                @PathVariable("source-id") UUID sourceId,
                                                                @RequestParam(value = "from_page", required = false) String fromPage,
                                                                @RequestParam(value = "to_page", required = false) String toPage) {

        int from = StringUtils.isEmpty(fromPage) ? Integer.parseInt(defaultFromPage) : Integer.parseInt(fromPage);
        int to = StringUtils.isEmpty(toPage) ? Integer.parseInt(defaultToPage) : Integer.parseInt(toPage);

        CollectingJobDto collectingJobDto = CollectingJobDto.of(sourceId, authentication.getUserId(), from, to);
        CollectingJobDto startedCollectingJobDto = collectingJobService.start(collectingJobDto);

        return ResponseEntity.ok(CollectingJobStartDto.Response.from(startedCollectingJobDto));
    }

    @Operation(
            summary = "수집 작업 종료",
            description = "CRON source의 자동 생성 사이클을 종료(isUsed=false). MANUAL source 호출시 무시됨."
    )
    @PostMapping("/sources/{source-id}/_stop")
    public ResponseEntity<Void> stop(@PathVariable("source-id") UUID sourceId) {

        collectingJobService.stop(sourceId);

        return ResponseEntity.accepted().build();
    }

    @Operation(summary = "수집 작업 상태 목록 조회")
    @GetMapping("/jobs")
    public ResponseEntity<OffsetPageResult<CollectingJobReadDto.Response>> getCollectingJobs(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                                                                             @RequestParam(value = "size", required = false, defaultValue = "10") int size) {

        OffsetPageResult<CollectingJobDto> collectSourceDtos = collectingJobService.getCollectingJobs(page, size);
        return ResponseEntity.ok(collectSourceDtos.map(CollectingJobReadDto.Response::from));
    }

    @Operation(summary = "수집 작업 상태 조회")
    @GetMapping("/jobs/{id}")
    public ResponseEntity<CollectingJobReadDto.Response> getSource(@PathVariable UUID id) {

        CollectingJobDto collectingJobDto = collectingJobService.getCollectingJobDto(id);
        return ResponseEntity.ok(CollectingJobReadDto.Response.from(collectingJobDto));
    }
}
