package com.backend.integratedworker.collectsourcepost.repository;

import com.backend.commondataaccess.persistence.collectingjob.QCollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.collectsource.QCollectSource;
import com.backend.commondataaccess.persistence.collectsource.QCollectSourcePost;
import com.backend.commondataaccess.persistence.common.enums.IndexingStatus;
import com.backend.commondataaccess.persistence.indexingjob.QIndexingJob;
import com.backend.commondataaccess.persistence.provider.QPostProvider;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CollectSourcePostQueryRepository {

    private final JPAQueryFactory queryFactory;

    private final QCollectSourcePost collectSourcePost = QCollectSourcePost.collectSourcePost;
    private final QCollectSource collectSource = QCollectSource.collectSource;
    private final QCollectingJob collectingJob = QCollectingJob.collectingJob;
    private final QIndexingJob indexingJob = QIndexingJob.indexingJob;
    private final QPostProvider postProvider = QPostProvider.postProvider;

    public Optional<CollectSourcePost> fetchOneById(UUID id) {
        CollectSourcePost result = queryFactory.select(collectSourcePost)
                                               .from(collectSourcePost)
                                               .join(collectSourcePost.collectSource, collectSource).fetchJoin()
                                               .join(collectSourcePost.collectSource.postProvider, postProvider).fetchJoin()
                                               .leftJoin(collectSourcePost.lastCollectingJob, collectingJob).fetchJoin()
                                               .leftJoin(collectSourcePost.lastIndexingJob, indexingJob).fetchJoin()
                                               .where(
                                                       collectSourcePost.id.eq(id),
                                                       collectSourcePost.deletedAt.isNull()
                                               )
                                               .fetchOne();

        return Optional.ofNullable(result);
    }

    public Optional<CollectSourcePost> fetchOneByUrl(String url) {
        CollectSourcePost result = queryFactory.select(collectSourcePost)
                                               .from(collectSourcePost)
                                               .join(collectSourcePost.collectSource, collectSource).fetchJoin()
                                               .join(collectSourcePost.collectSource.postProvider, postProvider).fetchJoin()
                                               .leftJoin(collectSourcePost.lastCollectingJob, collectingJob).fetchJoin()
                                               .leftJoin(collectSourcePost.lastIndexingJob, indexingJob).fetchJoin()
                                               .where(
                                                       collectSourcePost.url.eq(url),
                                                       collectSourcePost.deletedAt.isNull()
                                               )
                                               .fetchOne();

        return Optional.ofNullable(result);
    }

    /**
     * for IndexingJob
     */
    public List<CollectSourcePost> fetchIndexingCollectSourcePosts(UUID indexingJobId) {
        return queryFactory.select(collectSourcePost)
                           .from(collectSourcePost)
                           .join(collectSourcePost.collectSource, collectSource).fetchJoin()
                           .join(collectSourcePost.collectSource.postProvider, postProvider).fetchJoin()
                           .leftJoin(collectSourcePost.lastCollectingJob, collectingJob).fetchJoin()
                           .leftJoin(collectSourcePost.lastIndexingJob, indexingJob).fetchJoin()
                           .where(
                                   collectSourcePost.lastIndexingJob.id.eq(indexingJobId),
                                   collectSourcePost.indexingStatus.eq(IndexingStatus.INDEXING),
                                   collectSourcePost.deletedAt.isNull()
                           )
                           .fetch();
    }

    public List<CollectSourcePost> fetchReindexTargets(UUID sourceId) {
        return queryFactory.select(collectSourcePost)
                           .from(collectSourcePost)
                           .join(collectSourcePost.collectSource, collectSource).fetchJoin()
                           .join(collectSourcePost.collectSource.postProvider, postProvider).fetchJoin()
                           .leftJoin(collectSourcePost.lastCollectingJob, collectingJob).fetchJoin()
                           .leftJoin(collectSourcePost.lastIndexingJob, indexingJob).fetchJoin()
                           .where(
                                   collectSourcePost.collectSource.id.eq(sourceId),
                                   collectSourcePost.indexingStatus.in(IndexingStatus.INDEXED,
                                                                       IndexingStatus.FAILED,
                                                                       IndexingStatus.SKIPPED),
                                   collectSourcePost.deletedAt.isNull()
                           )
                           .fetch();
    }

    /**
     * INDEXING 상태로 갇혀 있는(=updatedAt이 threshold 이전인) post들을 일정 개수까지 조회. Reconciler가 PENDING으로 되돌리기 위해 사용.
     */
    public List<CollectSourcePost> fetchStaleIndexingTargets(OffsetDateTime updatedBefore, int limit) {
        return queryFactory.select(collectSourcePost)
                           .from(collectSourcePost)
                           .join(collectSourcePost.collectSource, collectSource).fetchJoin()
                           .join(collectSourcePost.collectSource.postProvider, postProvider).fetchJoin()
                           .leftJoin(collectSourcePost.lastCollectingJob, collectingJob).fetchJoin()
                           .leftJoin(collectSourcePost.lastIndexingJob, indexingJob).fetchJoin()
                           .where(
                                   collectSourcePost.indexingStatus.eq(IndexingStatus.INDEXING),
                                   collectSourcePost.updatedAt.lt(updatedBefore),
                                   collectSourcePost.deletedAt.isNull()
                           )
                           .limit(limit)
                           .fetch();
    }
}
