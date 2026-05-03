package com.backend.integratedworker.collectsourcepost.service;

import com.backend.commondataaccess.persistence.collectingjob.CollectingJob;
import com.backend.commondataaccess.persistence.collectsource.CollectSource;
import com.backend.commondataaccess.persistence.collectsource.CollectSourcePost;
import com.backend.integratedworker.collectingjob.service.dto.Post;
import com.backend.integratedworker.collectsourcepost.repository.CollectSourcePostQueryRepository;
import com.backend.integratedworker.collectsourcepost.repository.CollectSourcePostRepository;
import com.backend.integratedworker.collectsourcepost.service.validator.CollectSourcePostValidator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    }
}
