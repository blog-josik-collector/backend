package com.backend.integratedapi.provider.controller.dto;

import com.backend.integratedapi.provider.service.dto.PostProviderDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostProviderReadDto() {

    @Schema(description = "블로그 Provider 조회 결과")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record Response(
            @Schema(description = "Provider ID")
            UUID providerId,

            @Schema(description = "Provider 이름", example = "Toss")
            String name,

            @Schema(description = "기술 블로그 기본 URL", example = "https://toss.tech")
            String baseUrl,

            @Schema(description = "Provider 설명")
            String description,

            @Schema(description = "사용 여부")
            boolean isUsed,

            @Schema(description = "현재 사용 중인 Collect Source 존재 여부")
            boolean hasUsingCollectSource,

            @Schema(description = "현재 사용 중인 Collect Source ID")
            UUID usingCollectSourceId,

            @Schema(description = "생성 시각")
            OffsetDateTime createdAt,

            @Schema(description = "최종 수정 시각")
            OffsetDateTime updatedAt) {

        public static PostProviderReadDto.Response from(PostProviderDto postProviderDto) {
            return Response.builder()
                           .providerId(postProviderDto.id())
                           .name(postProviderDto.name())
                           .baseUrl(postProviderDto.baseUrl())
                           .description(postProviderDto.description())
                           .isUsed(postProviderDto.isUsed())
                           .hasUsingCollectSource(postProviderDto.hasUsingCollectSource())
                           .usingCollectSourceId(postProviderDto.usingCollectSourceId())
                           .createdAt(postProviderDto.createdAt())
                           .updatedAt(postProviderDto.updatedAt())
                           .build();
        }
    }
}
