package com.backend.integratedapi.provider.controller.dto;

import com.backend.integratedapi.provider.service.dto.PostProviderDto;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

public record PostProviderCreateDto() {

    public record Request(String name,
                          String baseUrl,
                          String description) {


    }

    @Builder
    public record Response(UUID providerId,
                           OffsetDateTime createdAt) {

        public static Response from(PostProviderDto dto) {
            return Response.builder()
                           .providerId(dto.id())
                           .createdAt(dto.createdAt())
                           .build();
        }

    }

}
