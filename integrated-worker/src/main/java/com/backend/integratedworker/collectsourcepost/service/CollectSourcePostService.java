package com.backend.integratedworker.collectsourcepost.service;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.commondataaccess.persistence.common.enums.IndexingStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import com.backend.integratedworker.collectsourcepost.repository.CollectSourcePostQueryRepository;
import com.backend.integratedworker.collectsourcepost.repository.CollectSourcePostRepository;
import com.backend.integratedworker.collectsourcepost.service.validator.CollectSourcePostValidator;
import com.backend.integratedworker.common.service.elasticsearch.dto.BulkIndexResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class CollectSourcePostService {

    private static final String HASH_FIELD_DELIMITER = "\u0001";
    private static final String HASH_NULL_PLACEHOLDER = "";
    private static final String HASH_ALGORITHM = "SHA-256";

    private final CollectSourcePostRepository collectSourcePostRepository;
    private final CollectSourcePostQueryRepository queryRepository;

    public CollectSourcePost create(Post post, CollectSource collectSource, CollectingJob collectingJob) {

        String contentHash = createContentHash(post);

        CollectSourcePost collectSourcePost = CollectSourcePost.builder()
                                                               .title(post.getTitle())
                                                               .url(post.getUrl())
                                                               .publishedAt(post.getPublishedAt())
                                                               .thumbnailUrl(post.getThumbnailUrl().isPresent() ? post.getThumbnailUrl().get() : null)
                                                               .summary(post.getSummary().isPresent() ? post.getSummary().get() : null)
                                                               .contentHash(contentHash)
                                                               .collectSource(collectSource)
                                                               .lastCollectingJob(collectingJob)
                                                               .indexingStatus(IndexingStatus.PENDING)
                                                               .build();

        return collectSourcePostRepository.save(collectSourcePost);
    }

    public String createContentHash(Post post) {
        String joined = String.join(HASH_FIELD_DELIMITER,
                                    normalize(post.getTitle()),
                                    normalize(post.getThumbnailUrl()),
                                    normalize(post.getSummary())
        );

        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(joined.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("지원하지 않는 해시 알고리즘입니다: " + HASH_ALGORITHM, e);
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return HASH_NULL_PLACEHOLDER;
        }
        return value.trim().replaceAll("\\s+", " "); // 정규식 '\s+'는 1개 이상의 연속된 공백 문자를 찾는 패턴
    }

    private String normalize(Optional<String> value) {
        return value.map(this::normalize).orElse(HASH_NULL_PLACEHOLDER);
    }

    @Transactional(readOnly = true)
    public CollectSourcePost getCollectSourcePost(UUID id) {
        return CollectSourcePostValidator.getCollectSourcePostOrThrow(id, queryRepository::fetchOneById);
    }

    @Transactional(readOnly = true)
    public CollectSourcePost getCollectSourcePost(String url) {
        CollectSourcePostValidator.validateUrl(url);
        return queryRepository.fetchOneByUrl(url).orElse(null);
    }

    public void update(UUID id, Post post, CollectingJob collectingJob) {
        CollectSourcePost collectSourcePost = getCollectSourcePost(id);
        String contentHash = createContentHash(post);

        collectSourcePost.updateTitle(post.getTitle());
        collectSourcePost.updatePublishedAt(post.getPublishedAt());
        collectSourcePost.updateThumbnailUrl(post.getThumbnailUrl().isPresent() ? post.getThumbnailUrl().get() : null);
        collectSourcePost.updateSummary(post.getSummary().isPresent() ? post.getSummary().get() : null);
        collectSourcePost.updateContentHash(contentHash);
        collectSourcePost.updateLastCollect(collectingJob, OffsetDateTime.now());

        //TODO: 재인덱싱 필요 상태로 되돌리기
        collectSourcePost.resetForReindex();
    }

    @Transactional(readOnly = true)
    public List<CollectSourcePost> pickPendingForIndexing(int batchSize) {
        return collectSourcePostRepository.findAllPendingCollectSourcePosts(batchSize);
    }

    @Transactional(readOnly = true)
    public List<CollectSourcePost> getIndexingCollectSourcePosts(UUID indexingJobId) {
        return queryRepository.fetchIndexingCollectSourcePosts(indexingJobId);
    }

    @Transactional(readOnly = true)
    public List<CollectSourcePost> getReindexTargetCollectSourcePosts(UUID sourceId) {
        return queryRepository.fetchReindexTargets(sourceId);
    }

    public void applyIndexResult(List<UUID> targetIds, BulkIndexResult result, IndexingJob job, OffsetDateTime now) {
        List<CollectSourcePost> posts = collectSourcePostRepository.findAllById(targetIds);

        if (posts.size() < targetIds.size()) {
            List<UUID> notExistedPostId = new ArrayList<>(targetIds);
            notExistedPostId.removeAll(posts.stream().map(CollectSourcePost::id).collect(Collectors.toSet()));
            notExistedPostId.forEach(postId -> {
                log.warn("applyIndexResult: CollectSourcePost not found. id={}, jobId={}", postId, job.id());
            });
        }

        for (CollectSourcePost p : posts) {
            if (result.isFailed(p.id())) {
                p.markIndexFailed(job);
            } else {
                p.markIndexed(job, now);
            }
        }
    }

    public List<CollectSourcePost> markIndexingBatch(IndexingJob job) {
        List<CollectSourcePost> targets = resolveManualTargets(job);

        for (CollectSourcePost collectSourcePost : targets) {
            collectSourcePost.markIndexing(job);
        }

        return targets;
    }

    private List<CollectSourcePost> resolveManualTargets(IndexingJob job) {
        if (job.targetSource() != null && job.targetPost() == null) {
            // collectSource 전체 재색인
            return getReindexTargetCollectSourcePosts(job.targetSource().id());
        }

        if (job.targetSource() == null && job.targetPost() != null) {
            // 단일 collectSourcePost 재색인.
            // job이 detached 상태로 넘어오므로 targetPost LAZY 프록시에 메서드 호출을 하면
            // LazyInitializationException이 난다. id만 꺼내서 현재 트랜잭션에서 fetch join 포함하여 재조회한다.
            return List.of(getCollectSourcePost(job.targetPost().id()));
        }

        throw new IllegalStateException(
                "MANUAL job 대상이 잘못됨: jobId=" + job.id()
                        + ", targetSource=" + job.targetSource()
                        + ", targetPost=" + job.targetPost()
        );
    }

    /**
     * INDEXING 상태로 갇힌 post들을 PENDING으로 되돌린다.
     * TX1 커밋 후 ~ TX2 커밋 전 사이에서 프로세스가 죽었을 때의 안전망.
     *
     * @return 복구된 row 개수
     */
    public int recoverStaleIndexing(OffsetDateTime updatedBefore, int limit) {
        List<CollectSourcePost> stalePosts = queryRepository.fetchStaleIndexingTargets(updatedBefore, limit);

        for (CollectSourcePost stalePost : stalePosts) {
            stalePost.resetForReindex();
        }

        return stalePosts.size();
    }
}
