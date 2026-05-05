package com.backend.integratedapi.indexingjob.controller;

import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.security.JwtPrincipal;
import com.backend.integratedapi.indexingjob.controller.dto.IndexingJobStartDto;
import com.backend.integratedapi.indexingjob.service.IndexingJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
