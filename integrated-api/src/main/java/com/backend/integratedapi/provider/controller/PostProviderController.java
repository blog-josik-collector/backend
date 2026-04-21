package com.backend.integratedapi.provider.controller;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.integratedapi.provider.controller.dto.PostProviderCreateDto;
import com.backend.integratedapi.provider.controller.dto.PostProviderReadDto;
import com.backend.integratedapi.provider.controller.dto.PostProviderUpdateDto;
import com.backend.integratedapi.provider.service.PostProviderService;
import com.backend.integratedapi.provider.service.dto.PostProviderDto;
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

@Tag(name = "01. Provider 관리 API")
@RequestMapping(value = "/collect/v1")
@RestController
@RequiredArgsConstructor
public class PostProviderController {

    private final PostProviderService postProviderService;

    @Operation(summary = "Provider 등록")
    @PostMapping("/providers")
    public ResponseEntity<PostProviderCreateDto.Response> create(@RequestBody PostProviderCreateDto.Request request) {

        PostProviderDto postProviderDto = PostProviderDto.of(request.name(), request.baseUrl(), request.description());

        PostProviderDto createdPostProviderDto = postProviderService.create(postProviderDto);

        return ResponseEntity.ok(PostProviderCreateDto.Response.from(createdPostProviderDto));
    }

    @Operation(summary = "Provider 목록 조회")
    @GetMapping("/providers")
    public ResponseEntity<OffsetPageResult<PostProviderReadDto.Response>> getProviders(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                                                                       @RequestParam(value = "size", required = false, defaultValue = "10") int size) {

        OffsetPageResult<PostProviderDto> postProviderDtos = postProviderService.getPostProviders(page, size);
        return ResponseEntity.ok(postProviderDtos.map(PostProviderReadDto.Response::from));
    }

    @Operation(summary = "Provider 한 건 조회")
    @GetMapping("/providers/{id}")
    public ResponseEntity<PostProviderReadDto.Response> getProvider(@PathVariable UUID id) {

        PostProviderDto postProviderDto = postProviderService.getPostProviderDto(id);
        return ResponseEntity.ok(PostProviderReadDto.Response.from(postProviderDto));
    }

    @Operation(summary = "Provider 수정")
    @PatchMapping("/providers/{id}")
    public ResponseEntity<PostProviderUpdateDto.Response> update(@PathVariable UUID id,
                                                                 @RequestBody PostProviderUpdateDto.Request request) {

        PostProviderDto postProviderDto = PostProviderDto.of(id, request.baseUrl(), request.description(), request.isUsed());
        postProviderService.update(postProviderDto);
        return ResponseEntity.ok(PostProviderUpdateDto.Response.from(postProviderService.getPostProviderDto(id)));
    }

    @Operation(summary = "Provider 삭제")
    @DeleteMapping("/providers/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {

        postProviderService.delete(id);
        return ResponseEntity.accepted().build();
    }
}
