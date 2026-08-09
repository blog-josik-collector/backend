package com.backend.integratedapi.indexedpost.controller;

import com.backend.integratedapi.indexedpost.service.IndexedPostService;
import com.backend.integratedapi.indexedpost.service.dto.IndexedPost;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "04. 색인된 Post 문서 조회 API")
@RequestMapping(value = "/index/v1/postings", produces = MediaType.APPLICATION_JSON_VALUE)
@RestController
@RequiredArgsConstructor
public class IndexedPostController {

    private final IndexedPostService indexedPostService;

    @Operation(summary = "색인 대상 문서 색인 상태 조회")
    @GetMapping("/{posting-id}")
    public ResponseEntity<IndexedPost> getIndexedPost(@PathVariable("posting-id") UUID postingId) {

        IndexedPost indexedPost = indexedPostService.getIndexedPost(postingId);
        return ResponseEntity.ok(indexedPost);
    }
}
