package com.backend.integratedapi.provider.controller.dto;

import com.backend.integratedapi.provider.service.dto.PostProviderDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostProviderReadDto() {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Builder
    public record Response(UUID providerId,
                           String name,
                           String baseUrl,
                           String description,
                           boolean isUsed,
                           boolean hasUsingCollectSource,
                           UUID usingCollectSourceId,
                           OffsetDateTime createdAt,
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
