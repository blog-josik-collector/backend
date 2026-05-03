package com.backend.integratedapi.collectsourcepost.controller;

import com.backend.integratedapi.collectsourcepost.controller.dto.CollectSourcePostReadDto;
import com.backend.integratedapi.collectsourcepost.service.CollectSourcePostService;
import com.backend.integratedapi.collectsourcepost.service.dto.CollectSourcePostDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "03. 수집 결과 조회 API")
@RequestMapping(value = "/collect/v1")
@RestController
@RequiredArgsConstructor
public class CollectSourcePostController {

    private final CollectSourcePostService collectSourcePostService;

    @Operation(summary = "수집 결과(원문/메타) 조회")
    @GetMapping("/postings/{id}")
    public ResponseEntity<CollectSourcePostReadDto.Response> getSource(@PathVariable UUID id) {

        CollectSourcePostDto collectSourcePostDto = collectSourcePostService.getCollectSourcePostDto(id);
        return ResponseEntity.ok(CollectSourcePostReadDto.Response.from(collectSourcePostDto));
    }
}
