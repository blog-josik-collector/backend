package com.backend.integratedapi.collectingjob.controller;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.integratedapi.collectingjob.controller.dto.CollectingJobReadDto;
import com.backend.integratedapi.collectingjob.controller.dto.CollectingJobStartDto;
import com.backend.integratedapi.collectingjob.service.CollectingJobService;
import com.backend.integratedapi.collectingjob.service.dto.CollectingJobDto;
import com.backend.integratedapi.collectsource.controller.dto.CollectSourceReadDto;
import com.backend.integratedapi.collectsourcepost.service.CollectSourcePostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "02. 수집 작업 실행 API")
@RequestMapping(value = "/collect/v1")
@RestController
@RequiredArgsConstructor
public class CollectingJobController {

    private final CollectingJobService collectingJobService;

    @Operation(summary = "수집 작업 실행")
    @PostMapping("/jobs")
    public ResponseEntity<CollectingJobStartDto.Response> start(@AuthenticationPrincipal JwtPrincipal authentication,
                                                                @RequestBody CollectingJobStartDto.Request request) {

        CollectingJobDto collectingJobDto = CollectingJobDto.of(request.sourceId(), authentication.getUserId());
        CollectingJobDto startedCollectingJobDto = collectingJobService.start(collectingJobDto);

        return ResponseEntity.ok(CollectingJobStartDto.Response.from(startedCollectingJobDto));
    }

    @Operation(summary = "수집 작업 종료")
    @PostMapping("/jobs/{job-id}/_stop")
    public ResponseEntity<OffsetPageResult<CollectSourceReadDto.Response>> stop(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                                                                @RequestParam(value = "size", required = false, defaultValue = "10") int size) {

        return null;
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
