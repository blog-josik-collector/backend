package com.backend.integratedapi.provider.controller.dto;

import com.backend.integratedapi.provider.service.dto.PostProviderDto;
import java.time.OffsetDateTime;
import lombok.Builder;

public record PostProviderCreateDto() {

    public record Request(String name,
                          String baseUrl,
                          String description) {


    }

    @Builder
    public record Response(String providerId,
                           OffsetDateTime createdAt) {

        public static Response from(PostProviderDto dto) {
            return Response.builder()
                           .providerId(dto.providerId().toString())
                           .createdAt(dto.createdAt())
                           .build();
        }

    }

}
