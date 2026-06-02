package com.backend.interactionservice.postlike.service;

import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.post.PostLike;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.service.PostService;
import com.backend.interactionservice.postlike.repository.PostLikeQueryRepository;
import com.backend.interactionservice.postlike.repository.PostLikeRepository;
import com.backend.interactionservice.postlike.service.validator.PostLikeValidator;
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
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostLikeQueryRepository queryRepository;
    private final PostQueryRepository postQueryRepository;
    private final PostService postService;
    private final UserService userService;

    /**
     * 좋아요를 누른다. 멱등하다.
     * <p>
     * - 활성 행이 없으면 INSERT 하고 like_count++
     * <p>
     * - 활성 행이 있고 is_enable=false 면 activate() 하고 like_count++
     * <p>
     * - 활성 행이 있고 is_enable=true 면 이미 좋아요 상태이므로 no-operation
     * <p>
     * - 동시 요청으로 unique index 충돌이 나면 멱등하게 무시한다.
     */
    public void like(UUID userId, UUID postId) {
        PostLikeValidator.validateUserId(userId);
        PostLikeValidator.validatePostId(postId);

        Optional<PostLike> existing = queryRepository.fetchOneByUserAndPost(userId, postId);

        if (existing.isPresent()) {
            PostLike postLike = existing.get();
            if (postLike.isEnable()) {
                return;
            }
            postLike.activate();
            postQueryRepository.incrementLikeCount(postId);
            return;
        }

        try {
            User user = userService.getUser(userId);
            Post post = postService.getPost(postId);

            PostLike postLike = PostLike.builder()
                                        .user(user)
                                        .post(post)
                                        .isEnable(true)
                                        .build();

            postLikeRepository.save(postLike);
            postQueryRepository.incrementLikeCount(postId);

        } catch (DataIntegrityViolationException e) {
            // 동일 (userId, postId)에 대한 동시 좋아요 요청 → 부분 unique index에 의해 한쪽만 성공
            // 실패한 쪽은 멱등하게 종료. like_count는 성공한 쪽에서 이미 증가시켰다.
            log.debug("[PostLike] duplicate like, idempotent userId={} postId={}", userId, postId);
        }
    }

    /**
     * 좋아요를 취소한다. 멱등하다.
     * <p>
     * - 활성 행이 없거나 is_enable=false 면 no-operation
     * <p>
     * - 활성 행이 있고 is_enable=true 면 deactivate() 하고 like_count--
     * <p>
     * 행 자체는 soft-delete 하지 않는다. deleted_at은 관리자/정책에 의한 진짜 삭제 시맨틱으로 남겨둔다.
     */
    public void unLike(UUID userId, UUID postId) {
        PostLikeValidator.validateUserId(userId);
        PostLikeValidator.validatePostId(postId);

        PostLike postLike = PostLikeValidator.getPostLikeOrThrow(userId, postId, queryRepository::fetchOneByUserAndPost);

        if (!postLike.isEnable()) {
            return;
        }

        postLike.deactivate();
        postQueryRepository.decrementLikeCount(postId);
    }
}
