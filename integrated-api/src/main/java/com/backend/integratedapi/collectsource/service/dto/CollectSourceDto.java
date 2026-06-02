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
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CollectSourceDto {

    private UUID providerId;
    private String url;
    private CollectScheduleType collectScheduleType;
    private String cronExpression;
    private Integer cronFromPage;
    private Integer cronToPage;
    private boolean isUsed;

    private UUID id;

    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static CollectSourceDto of(UUID providerId,
                                      String url,
                                      CollectScheduleType collectScheduleType,
                                      String cronExpression) {
        return of(providerId, url, collectScheduleType, cronExpression, null, null);
    }

    public static CollectSourceDto of(UUID providerId,
                                      String url,
                                      CollectScheduleType collectScheduleType,
                                      String cronExpression,
                                      Integer cronFromPage,
                                      Integer cronToPage) {
        return CollectSourceDto.builder()
                               .providerId(providerId)
                               .url(url)
                               .collectScheduleType(collectScheduleType)
                               .cronExpression(cronExpression)
                               .cronFromPage(cronFromPage)
                               .cronToPage(cronToPage)
                               .isUsed(true)
                               .build();
    }

    public static CollectSourceDto of(UUID id,
                                      String url,
                                      CollectScheduleType collectScheduleType,
                                      String cronExpression,
                                      Boolean isUsed) {
        return of(id, url, collectScheduleType, cronExpression, null, null, isUsed);
    }

    public static CollectSourceDto of(UUID id,
                                      String url,
                                      CollectScheduleType collectScheduleType,
                                      String cronExpression,
                                      Integer cronFromPage,
                                      Integer cronToPage,
                                      Boolean isUsed) {
        return CollectSourceDto.builder()
                               .id(id)
                               .url(url)
                               .collectScheduleType(collectScheduleType)
                               .cronExpression(cronExpression)
                               .cronFromPage(cronFromPage)
                               .cronToPage(cronToPage)
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
                               .cronFromPage(collectSource.cronFromPage())
                               .cronToPage(collectSource.cronToPage())
                               .isUsed(collectSource.isUsed())
                               .createdAt(collectSource.createdAt())
                               .updatedAt(collectSource.updatedAt())
                               .build();
    }
}
