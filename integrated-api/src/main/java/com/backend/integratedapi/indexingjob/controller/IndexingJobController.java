package com.backend.integratedapi.indexingjob.controller;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.integratedapi.indexingjob.controller.dto.IndexingJobReadDto;
import com.backend.integratedapi.indexingjob.controller.dto.IndexingJobStartDto;
import com.backend.integratedapi.indexingjob.service.IndexingJobService;
import com.backend.integratedapi.indexingjob.service.dto.IndexingJobDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "03. 색인 작업 실행 API")
@RequestMapping(value = "/index/v1")
@RestController
@RequiredArgsConstructor
public class IndexingJobController {

    private final IndexingJobService indexingJobService;

    @Operation(summary = "이 source의 전체 post 재색인")
    @PostMapping("/sources/{source-id}/_reindex")
    public ResponseEntity<IndexingJobStartDto.Response> reindexSource(@AuthenticationPrincipal JwtPrincipal principal,
                                                                      @PathVariable("source-id") UUID sourceId) {

        UUID jobId = indexingJobService.triggerReindexByCollectSource(sourceId, principal.getUserId()).id();
        return ResponseEntity.accepted().body(new IndexingJobStartDto.Response(jobId, JobStatus.PENDING));
    }

    @Operation(summary = "특정 post 1개 재색인")
    @PostMapping("/posts/{post-id}/_reindex")
    public ResponseEntity<IndexingJobStartDto.Response> reindexPost(@AuthenticationPrincipal JwtPrincipal principal,
                                                                    @PathVariable("post-id") UUID postId) {

        UUID jobId = indexingJobService.triggerReindexByCollectSourcePost(postId, principal.getUserId()).id();
        return ResponseEntity.accepted().body(new IndexingJobStartDto.Response(jobId, JobStatus.PENDING));
    }

    @Operation(summary = "색인 작업 상태 목록 조회")
    @GetMapping("/jobs")
    public ResponseEntity<OffsetPageResult<IndexingJobReadDto.Response>> getIndexingJobs(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                                                                         @RequestParam(value = "size", required = false, defaultValue = "10") int size) {

        OffsetPageResult<IndexingJobDto> indexingJobDtos = indexingJobService.getIndexingJobs(page, size);
        return ResponseEntity.ok(indexingJobDtos.map(IndexingJobReadDto.Response::from));
    }

    @Operation(summary = "색인 작업 상태 조회")
    @GetMapping("/jobs/{id}")
    public ResponseEntity<IndexingJobReadDto.Response> getIndexingJob(@PathVariable UUID id) {

        IndexingJobDto indexingJobDto = indexingJobService.getIndexingJobDto(id);
        return ResponseEntity.ok(IndexingJobReadDto.Response.from(indexingJobDto));
    }
}
