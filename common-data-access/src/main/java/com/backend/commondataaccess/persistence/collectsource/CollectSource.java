package com.backend.commondataaccess.persistence.collectsource;

import com.backend.commondataaccess.persistence.BaseEntity;
import com.backend.commondataaccess.persistence.provider.PostProvider;
import com.backend.commondataaccess.persistence.collectsource.enums.ScheduleType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "collect_sources")
@Entity
public class CollectSource extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_provider_id", nullable = false)
    private PostProvider postProvider;

    private String url;

    private ScheduleType scheduleType;

    private String cronExpression;

    private boolean isUsed;

    public void updateUrl(String url) {
        this.url = url;
    }

    public void updateScheduleType(ScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public void updateCronExpression(String cronExpression) {
        this.cronExpression = cronExpression;
    }

    public void updateUsed(boolean isUsed) {
        this.isUsed = isUsed;
    }
}
