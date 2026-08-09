package com.backend.integratedapi.provider.controller.dto;

import com.backend.integratedapi.provider.service.dto.PostProviderDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostProviderCreateDto() {

    @Schema(description = "블로그 Provider 생성 요청")
    public record Request(
            @Schema(description = "Provider 이름", example = "Toss")
            String name,

            @Schema(description = "기술 블로그 기본 URL", example = "https://toss.tech")
            String baseUrl,

            @Schema(description = "Provider 설명", example = "토스 기술 블로그")
            String description) {


    }

    @Schema(description = "블로그 Provider 생성 결과")
    @Builder
    public record Response(
            @Schema(description = "생성된 Provider ID")
            UUID providerId,

            @Schema(description = "생성 시각")
            OffsetDateTime createdAt) {

        public static Response from(PostProviderDto dto) {
            return Response.builder()
                           .providerId(dto.id())
                           .createdAt(dto.createdAt())
                           .build();
        }

    }

}
