package com.backend.integratedapi.elasticsearchindex.controller;

import com.backend.commonelasticsearch.provision.ElasticsearchIndexProvisioner;
import com.backend.commonelasticsearch.provision.ProvisionResult;
import com.backend.commonelasticsearch.provision.ReindexResult;
import com.backend.integratedapi.elasticsearchindex.controller.dto.ElasticsearchIndexReadDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Elasticsearch 인덱스 라이프사이클(생성/재색인/상태) 관리자 API. <br>
 * {@code /index/v1/**} 는 ADMIN 권한 필요(SecurityConfig).
 */
@Tag(name = "05. Elasticsearch 인덱스 관리 API")
@RequestMapping(value = "/index/v1/elasticsearch")
@RestController
@RequiredArgsConstructor
public class ElasticsearchIndexAdminController {

    private final ElasticsearchIndexProvisioner provisioner;

    @Operation(summary = "alias 현재 상태 조회 (alias → 물리 인덱스)")
    @GetMapping("/status")
    public ResponseEntity<ElasticsearchIndexReadDto.Status> getStatus() {
        String alias = provisioner.alias();
        String currentIndex = provisioner.currentIndex().orElse(null);
        return ResponseEntity.ok(new ElasticsearchIndexReadDto.Status(alias, currentIndex, currentIndex != null));
    }

    @Operation(summary = "인덱스 부트스트랩 (alias 없으면 물리 인덱스 + alias 생성)")
    @PostMapping("/_bootstrap")
    public ResponseEntity<ElasticsearchIndexReadDto.Bootstrap> bootstrap() {
        ProvisionResult result = provisioner.bootstrapIfAbsent();
        return ResponseEntity.ok(ElasticsearchIndexReadDto.Bootstrap.from(result));
    }

    @Operation(summary = "재색인 실행 (새 물리 인덱스 생성 → reindex → alias 원자적 스왑)")
    @PostMapping("/_reindex")
    public ResponseEntity<ElasticsearchIndexReadDto.Reindex> reindex() {
        ReindexResult result = provisioner.reindexToNewIndex();
        return ResponseEntity.ok(ElasticsearchIndexReadDto.Reindex.from(result));
    }
}
