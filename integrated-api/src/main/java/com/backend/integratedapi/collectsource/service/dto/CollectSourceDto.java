package com.backend.integratedapi.collectsource.service.dto;

import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.enums.ScheduleType;
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
    private ScheduleType scheduleType;
    private String cronExpression;
    private boolean isUsed;

    private UUID id;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CollectSourceDto of(UUID providerId, String url, ScheduleType scheduleType, String cronExpression) {
        return CollectSourceDto.builder()
                               .providerId(providerId)
                               .url(url)
                               .scheduleType(scheduleType)
                               .cronExpression(cronExpression)
                               .isUsed(true)
                               .build();
    }

    public static CollectSourceDto of(UUID id, String url, ScheduleType scheduleType, String cronExpression, Boolean isUsed) {
        return CollectSourceDto.builder()
                               .id(id)
                               .url(url)
                               .scheduleType(scheduleType)
                               .cronExpression(cronExpression)
                               .isUsed(isUsed == null || isUsed)
                               .build();
    }

    public static CollectSourceDto from(CollectSource collectSource) {
        return CollectSourceDto.builder()
                               .id(collectSource.id())
                               .providerId(collectSource.postProvider().id())
                               .url(collectSource.url())
                               .scheduleType(collectSource.scheduleType())
                               .cronExpression(collectSource.cronExpression())
                               .isUsed(collectSource.isUsed())
                               .createdAt(collectSource.createdAt())
                               .updatedAt(collectSource.updatedAt())
                               .build();
    }
}
