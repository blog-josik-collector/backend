package com.backend.integratedapi.collectsource.service.dto;

import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.enums.CollectScheduleType;
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
public class CollectSourceDto {

    private UUID providerId;
    private String url;
    private CollectScheduleType collectScheduleType;
    private String cronExpression;
    private boolean isUsed;

    private UUID id;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CollectSourceDto of(UUID providerId, String url, CollectScheduleType collectScheduleType, String cronExpression) {
        return CollectSourceDto.builder()
                               .providerId(providerId)
                               .url(url)
                               .collectScheduleType(collectScheduleType)
                               .cronExpression(cronExpression)
                               .isUsed(true)
                               .build();
    }

    public static CollectSourceDto of(UUID id, String url, CollectScheduleType collectScheduleType, String cronExpression, Boolean isUsed) {
        return CollectSourceDto.builder()
                               .id(id)
                               .url(url)
                               .collectScheduleType(collectScheduleType)
                               .cronExpression(cronExpression)
                               .isUsed(isUsed == null || isUsed)
                               .build();
    }

    public static CollectSourceDto from(CollectSource collectSource) {
        return CollectSourceDto.builder()
                               .id(collectSource.id())
                               .providerId(collectSource.postProvider().id())
                               .url(collectSource.url())
                               .collectScheduleType(collectSource.collectScheduleType())
                               .cronExpression(collectSource.cronExpression())
                               .isUsed(collectSource.isUsed())
                               .createdAt(collectSource.createdAt())
                               .updatedAt(collectSource.updatedAt())
                               .build();
    }
}
