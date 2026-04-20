package com.backend.integratedapi.provider.controller.dto;

import com.backend.integratedapi.provider.service.dto.PostProviderDto;
import java.time.OffsetDateTime;
import lombok.Builder;

public record PostProviderReadDto() {

    @Builder
    public record Response(String providerId,
                           String name,
                           String baseUrl,
                           String description,
                           boolean isUsed,
                           OffsetDateTime createdAt,
                           OffsetDateTime updatedAt) {

        public static PostProviderReadDto.Response from(PostProviderDto postProviderDto) {
            return Response.builder()
                           .providerId(postProviderDto.providerId().toString())
                           .name(postProviderDto.name())
                           .baseUrl(postProviderDto.baseUrl())
                           .description(postProviderDto.description())
                           .isUsed(postProviderDto.isUsed())
                           .createdAt(postProviderDto.createdAt())
                           .updatedAt(postProviderDto.updatedAt())
                           .build();
        }
    }
}
