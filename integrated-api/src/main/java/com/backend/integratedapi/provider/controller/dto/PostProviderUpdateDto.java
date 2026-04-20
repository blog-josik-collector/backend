package com.backend.integratedapi.provider.controller.dto;

import com.backend.integratedapi.provider.service.dto.PostProviderDto;
import java.time.OffsetDateTime;
import lombok.Builder;

public record PostProviderUpdateDto() {

    public record Request(String baseUrl,
                          String description,
                          Boolean isUsed) {

    }

    @Builder
    public record Response(String providerId,
                           OffsetDateTime updatedAt) {

        public static Response from(PostProviderDto dto) {
            return Response.builder()
                           .providerId(dto.providerId().toString())
                           .updatedAt(dto.updatedAt())
                           .build();
        }

    }

}
