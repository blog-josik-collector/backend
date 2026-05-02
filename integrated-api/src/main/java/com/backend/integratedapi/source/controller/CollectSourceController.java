package com.backend.integratedapi.source.controller;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.integratedapi.source.controller.dto.CollectSourceCreateDto;
import com.backend.integratedapi.source.controller.dto.CollectSourceReadDto;
import com.backend.integratedapi.source.controller.dto.CollectSourceUpdateDto;
import com.backend.integratedapi.source.service.CollectSourceService;
import com.backend.integratedapi.source.service.dto.CollectSourceDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "02. 수집 소스 관리 API")
@RequestMapping(value = "/collect/v1")
@RestController
@RequiredArgsConstructor
public class CollectSourceController {

    private final CollectSourceService collectSourceService;

    @Operation(summary = "수집 소스 등록")
    @PostMapping("/sources")
    public ResponseEntity<CollectSourceCreateDto.Response> create(@RequestBody CollectSourceCreateDto.Request request) {

        CollectSourceDto collectSourceDto = CollectSourceDto.of(request.providerId(), request.url(), request.scheduleType(), request.cronExpression());
        CollectSourceDto createdPostProviderDto = collectSourceService.create(collectSourceDto);

        return ResponseEntity.ok(CollectSourceCreateDto.Response.from(createdPostProviderDto));
    }

    @Operation(summary = "수집 소스 목록 조회")
    @GetMapping("/sources")
    public ResponseEntity<OffsetPageResult<CollectSourceReadDto.Response>> getSources(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                                                                      @RequestParam(value = "size", required = false, defaultValue = "10") int size) {

        OffsetPageResult<CollectSourceDto> collectSourceDtos = collectSourceService.getCollectSources(page, size);
        return ResponseEntity.ok(collectSourceDtos.map(CollectSourceReadDto.Response::from));
    }

    @Operation(summary = "수집 소스 한 건 조회")
    @GetMapping("/sources/{id}")
    public ResponseEntity<CollectSourceReadDto.Response> getSource(@PathVariable UUID id) {

        CollectSourceDto collectSourceDto = collectSourceService.getCollectSourceDto(id);
        return ResponseEntity.ok(CollectSourceReadDto.Response.from(collectSourceDto));
    }

    @Operation(summary = "수집 소스 수정")
    @PatchMapping("/sources/{id}")
    public ResponseEntity<CollectSourceUpdateDto.Response> update(@PathVariable UUID id,
                                                                  @RequestBody CollectSourceUpdateDto.Request request) {

        CollectSourceDto collectSourceDto = CollectSourceDto.of(id, request.url(), request.scheduleType(), request.cronExpression(), request.isUsed());
        collectSourceService.update(collectSourceDto);
        return ResponseEntity.ok(CollectSourceUpdateDto.Response.from(collectSourceService.getCollectSourceDto(id)));
    }

    @Operation(summary = "수집 소스 삭제")
    @DeleteMapping("/sources/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {

        collectSourceService.delete(id);
        return ResponseEntity.accepted().build();
    }

}
