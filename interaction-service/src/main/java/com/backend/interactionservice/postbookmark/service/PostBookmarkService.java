package com.backend.interactionservice.postbookmark.service;

import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.post.PostBookmark;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.interactionservice.post.service.PostService;
import com.backend.interactionservice.postbookmark.repository.PostBookmarkQueryRepository;
import com.backend.interactionservice.postbookmark.repository.PostBookmarkRepository;
import com.backend.interactionservice.postbookmark.service.validator.PostBookmarkValidator;
import com.backend.interactionservice.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class PostBookmarkService {

    private final PostBookmarkRepository postBookmarkRepository;
    private final PostBookmarkQueryRepository queryRepository;
    private final PostService postService;
    private final UserService userService;

    /**
     * 즐겨찾기를 한다. 멱등하다.
     * <p>
     * - 활성 행이 없으면 INSERT
     * <p>
     * - 활성 행이 있고 is_enable=false면 activate()
     * <p>
     * - 활성 행이 있고 is_enable=true 면 이미 좋아요 상태이므로 no-operation
     * <p>
     * - 동시 요청으로 unique index 충돌이 나면 멱등하게 무시한다.
     */
    public void bookmark(UUID userId, UUID postId) {
        PostBookmarkValidator.validateUserId(userId);
        PostBookmarkValidator.validatePostId(postId);

        Optional<PostBookmark> existing = queryRepository.fetchOneByUserAndPost(userId, postId);

        if (existing.isPresent()) {
            PostBookmark postBookmark = existing.get();
            if (postBookmark.isEnable()) {
                return;
            }
            postBookmark.activate();
            return;
        }

        try {
            User user = userService.getUser(userId);
            Post post = postService.getPost(postId);

            PostBookmark postBookmark = PostBookmark.builder()
                                                    .user(user)
                                                    .post(post)
                                                    .isEnable(true)
                                                    .build();

            postBookmarkRepository.save(postBookmark);

        } catch (DataIntegrityViolationException e) {
            // 동일 (userId, postId)에 대한 동시 좋아요 요청 → 부분 unique index에 의해 한쪽만 성공
            // 실패한 쪽은 멱등하게 종료. like_count는 성공한 쪽에서 이미 증가시켰다.
            log.debug("Duplicate like detected, treating as idempotent. userId={}, postId={}", userId, postId);
        }
    }

    /**
     * 즐겨찾기를 취소한다. 멱등하다.
     * <p>
     * - 활성 행이 없거나 is_enable=false 면 no-operation
     * <p>
     * - 활성 행이 있고 is_enable=true 면 deactivate()
     * <p>
     * 행 자체는 soft-delete 하지 않는다. deleted_at은 관리자/정책에 의한 진짜 삭제 시맨틱으로 남겨둔다.
     */
    public void unBookmark(UUID userId, UUID postId) {
        PostBookmarkValidator.validateUserId(userId);
        PostBookmarkValidator.validatePostId(postId);

        PostBookmark postBookmark = PostBookmarkValidator.getPostBookmarkOrThrow(userId, postId, queryRepository::fetchOneByUserAndPost);

        if (!postBookmark.isEnable()) {
            return;
        }

        postBookmark.deactivate();
    }
}
