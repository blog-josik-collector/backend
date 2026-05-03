package com.backend.integratedapi.provider.service.dto;

import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Accessors(fluent = true)
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 빌더용
@NoArgsConstructor(access = AccessLevel.PRIVATE)  // Jackson 역직렬화용
public class PostProviderDto {

    private String name;
    private String baseUrl;
    private String description;
    private boolean isUsed;
    private boolean hasUsingCollectSource;
    private UUID usingCollectSourceId;

    private UUID id;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    // for create
    public static PostProviderDto of(String name, String baseUrl, String description) {
        return PostProviderDto.builder()
                              .name(name)
                              .baseUrl(baseUrl)
                              .description(description)
                              .isUsed(true)
                              .build();
    }

    // for create
    public static PostProviderDto of(String name, String baseUrl, String description, Boolean isUsed) {
        return PostProviderDto.builder()
                              .name(name)
                              .baseUrl(baseUrl)
                              .description(description)
                              .isUsed(isUsed == null || isUsed)
                              .build();
    }

    // for update
    public static PostProviderDto of(UUID id, String baseUrl, String description, Boolean isUsed) {
        return PostProviderDto.builder()
                              .id(id)
                              .baseUrl(baseUrl)
                              .description(description)
                              .isUsed(isUsed == null || isUsed)
                              .build();
    }

    public static PostProviderDto from(PostProvider postProvider) {
        CollectSource collectSource = postProvider.collectSources().stream().filter(CollectSource::isUsed).findFirst().orElse(null);

        return PostProviderDto.builder()
                              .id(postProvider.id())
                              .name(postProvider.name())
                              .baseUrl(postProvider.baseUrl())
                              .description(postProvider.description())
                              .isUsed(postProvider.isUsed())
                              .hasUsingCollectSource(collectSource != null)
                              .usingCollectSourceId(collectSource == null ? null : collectSource.id())
                              .createdAt(postProvider.createdAt())
                              .updatedAt(postProvider.updatedAt())
                              .build();
    }
}
