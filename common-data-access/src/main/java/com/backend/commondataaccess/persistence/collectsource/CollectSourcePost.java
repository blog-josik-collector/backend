package com.backend.commondataaccess.persistence.collectsource;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.common.BaseEntity;
import com.backend.commondataaccess.persistence.common.enums.IndexingStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
@Table(name = "collect_source_posts")
@Entity
public class CollectSourcePost extends BaseEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collect_source_id", nullable = false)
    private CollectSource collectSource;

    private String title;

    private String url;

    private LocalDate publishedAt; // 발행일

    private String thumbnailUrl;  // 썸네일 이미지

    private String summary; // 요약

    private String content;

    private String contentHash;

    @Enumerated(EnumType.STRING)
    private IndexingStatus indexingStatus;

    private int indexingErrorCount;

    private OffsetDateTime lastIndexedAt;

    private OffsetDateTime lastCollectedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_collecting_job_id", nullable = false)
    private CollectingJob lastCollectingJob;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "last_indexing_job_id")
    private IndexingJob lastIndexingJob;

    public void updateTitle(String title) {
        this.title = title;
    }

    public void updatePublishedAt(LocalDate publishedAt) {
        this.publishedAt = publishedAt;
    }

    public void updateThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public void updateSummary(String summary) {
        this.summary = summary;
    }

    public void updateContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public void updateLastCollect(CollectingJob collectingJob, OffsetDateTime offsetDateTime) {
        this.lastCollectingJob = collectingJob;
        this.lastCollectedAt = offsetDateTime;
    }

    /**
     * for Indexing
     */
    public void markIndexing(IndexingJob indexingJob) {
        this.indexingStatus = IndexingStatus.INDEXING;
        this.lastIndexingJob = indexingJob;
    }

    public void markIndexed(IndexingJob indexingJob, OffsetDateTime now) {
        this.indexingStatus = IndexingStatus.INDEXED;
        this.lastIndexedAt = now;
        this.lastIndexingJob = indexingJob;
        this.indexingErrorCount = 0;
    }

    public void markIndexFailed(IndexingJob indexingJob) {
        this.indexingStatus = IndexingStatus.FAILED;
        this.lastIndexingJob = indexingJob;
        this.indexingErrorCount++;
    }

    public void markIndexSkipped(IndexingJob indexingJob, OffsetDateTime now) {
        this.indexingStatus = IndexingStatus.SKIPPED;
        this.lastIndexedAt = now;
        this.lastIndexingJob = indexingJob;
    }

    public void resetForReindex() {
        this.indexingStatus = IndexingStatus.PENDING;
        this.indexingErrorCount = 0;
    }
}
