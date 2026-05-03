package com.backend.integratedapi.collectingjob.controller;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.integratedapi.collectingjob.controller.dto.CollectingJobStartDto;
import com.backend.integratedapi.collectingjob.service.CollectingJobService;
import com.backend.integratedapi.collectingjob.service.dto.CollectingJobDto;
import com.backend.integratedapi.collectsource.controller.dto.CollectSourceCreateDto;
import com.backend.integratedapi.collectsource.controller.dto.CollectSourceReadDto;
import com.backend.integratedapi.collectsource.service.dto.CollectSourceDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
