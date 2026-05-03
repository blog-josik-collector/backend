package com.backend.commondataaccess.persistence.provider;

import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
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
@Table(name = "post_providers")
@Entity
public class PostProvider extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;

    private String description;

    private String baseUrl;

    private boolean isUsed;

    @OneToMany(mappedBy = "postProvider")
    private List<CollectSource> collectSources = new ArrayList<>();

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public void updateUsed(boolean isUsed) {
        this.isUsed = isUsed;
    }
}
