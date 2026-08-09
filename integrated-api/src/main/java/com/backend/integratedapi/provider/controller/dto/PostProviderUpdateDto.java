package com.backend.integratedapi.provider.controller.dto;

import com.backend.integratedapi.provider.service.dto.PostProviderDto;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostProviderUpdateDto() {

    @Schema(description = "블로그 Provider 수정 요청")
    public record Request(
            @Schema(description = "기술 블로그 기본 URL", example = "https://toss.tech")
            String baseUrl,

            @Schema(description = "Provider 설명")
            String description,

            @Schema(description = "사용 여부")
            Boolean isUsed) {

    }

    @Schema(description = "블로그 Provider 수정 결과")
    @Builder
    public record Response(
            @Schema(description = "Provider ID")
            UUID providerId,

            @Schema(description = "수정 시각")
            OffsetDateTime updatedAt) {

        public static Response from(PostProviderDto dto) {
            return Response.builder()
                           .providerId(dto.id())
                           .updatedAt(dto.updatedAt())
                           .build();
        }

    }

}
