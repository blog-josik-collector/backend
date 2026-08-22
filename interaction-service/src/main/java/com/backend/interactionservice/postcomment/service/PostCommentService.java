package com.backend.interactionservice.postcomment.service;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.PostCommentStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.post.PostComment;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.service.PostService;
import com.backend.interactionservice.postcomment.repository.PostCommentQueryRepository;
import com.backend.interactionservice.postcomment.repository.PostCommentRepository;
import com.backend.interactionservice.postcomment.service.dto.PostCommentDto;
import com.backend.interactionservice.postcomment.service.validator.PostCommentValidator;
import com.backend.interactionservice.user.service.UserService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final PostCommentRepository postCommentRepository;
    private final PostCommentQueryRepository queryRepository;
    private final PostQueryRepository postQueryRepository;
    private final PostService postService;
    private final UserService userService;

    /**
     * 활성 상태인 댓글/대댓글 단건을 가져온다. 다른 도메인(예: 댓글 신고)에서 PostComment 인스턴스가 필요할 때 사용한다.
     */
    @Transactional(readOnly = true)
    public PostComment getComment(UUID commentId) {
        return PostCommentValidator.getPostCommentOrThrow(commentId, queryRepository::fetchOneById);
    }

    /**
     * 1. 댓글 작성. 1-depth 댓글로 만든다.
     */
    public PostCommentDto createComment(UUID userId, UUID postId, String content) {
        PostCommentValidator.validateUserId(userId);
        PostCommentValidator.validatePostId(postId);
        PostCommentValidator.validateContent(content);

        User user = userService.getUser(userId);
        Post post = postService.getPost(postId);

        PostComment comment = PostComment.builder()
                                         .user(user)
                                         .post(post)
                                         .parentComment(null)
                                         .content(content)
                                         .postCommentStatus(PostCommentStatus.ACTIVE)
                                         .build();

        PostComment saved = postCommentRepository.save(comment);
        postQueryRepository.incrementCommentCount(postId);
        return PostCommentDto.from(saved);
    }

    /**
     * 2. 포스팅의 댓글(1-depth) 목록 조회. 각 댓글에 대해 자식 댓글(대댓글, soft-delete 포함) 존재 여부(has_child_comment)를 함께 채워서 반환한다.
     */
    @Transactional(readOnly = true)
    public OffsetPageResult<PostCommentDto> getComments(UUID postId, Pageable pageable) {
        PostCommentValidator.validatePostId(postId);

        OffsetPageResult<PostComment> page = queryRepository.fetchCommentsByPostId(postId, pageable);
        return mapWithHasChildComment(page);
    }

    /**
     * 3. 댓글 수정. 작성자 본인만 가능, 대상은 1-depth 댓글이어야 한다.
     */
    public PostCommentDto updateComment(UUID userId, UUID commentId, String content) {
        PostCommentValidator.validateContent(content);

        PostComment comment = PostCommentValidator.getPostCommentOrThrow(commentId, queryRepository::fetchOneById);
        PostCommentValidator.validateIsComment(comment);
        PostCommentValidator.validateOwnership(comment, userId);

        comment.updateContent(content);
        return PostCommentDto.from(comment);
    }

    /**
     * 4. 댓글 삭제. 작성자 본인만 가능, 대상은 1-depth 댓글이어야 한다. soft-delete 한다.
     */
    public void deleteComment(UUID userId, UUID commentId) {
        PostComment comment = PostCommentValidator.getPostCommentOrThrow(commentId, queryRepository::fetchOneById);
        PostCommentValidator.validateIsComment(comment);
        PostCommentValidator.validateOwnership(comment, userId);

        comment.softDelete();
        postQueryRepository.decrementCommentCount(comment.post().id());
    }

    /**
     * 5. 대댓글 작성. 부모는 어떤 댓글이든(최상위 댓글이든 다른 대댓글이든) 될 수 있다. 부모-자식 관계 자체는 2-depth이지만 트리 전체 깊이는 제한하지 않는다.
     */
    public PostCommentDto createReply(UUID userId, UUID parentCommentId, String content) {
        PostCommentValidator.validateUserId(userId);
        PostCommentValidator.validateContent(content);

        PostComment parent = PostCommentValidator.getPostCommentOrThrow(parentCommentId, queryRepository::fetchOneById);

        User user = userService.getUser(userId);

        PostComment reply = PostComment.builder()
                                       .user(user)
                                       .post(parent.post())
                                       .parentComment(parent)
                                       .content(content)
                                       .postCommentStatus(PostCommentStatus.ACTIVE)
                                       .build();

        PostComment saved = postCommentRepository.save(reply);
        postQueryRepository.incrementCommentCount(parent.post().id());
        return PostCommentDto.from(saved);
    }

    /**
     * 6. 특정 댓글의 자식 댓글 목록 조회. 부모가 최상위 댓글이든 다른 대댓글이든 동일하게 직접 자식들만 반환한다. 각 항목에 대해 또 다른 자식이 달려있는지 여부(has_child_comment, soft-delete 포함)도 함께 계산한다.
     */
    @Transactional(readOnly = true)
    public OffsetPageResult<PostCommentDto> getReplies(UUID parentCommentId, Pageable pageable) {
        PostCommentValidator.validateCommentId(parentCommentId);

        OffsetPageResult<PostComment> page = queryRepository.fetchRepliesByParentId(parentCommentId, pageable);
        return mapWithHasChildComment(page);
    }

    /**
     * 7. 대댓글 수정. 작성자 본인만 가능, 대상은 2-depth 대댓글이어야 한다.
     */
    public PostCommentDto updateReply(UUID userId, UUID replyId, String content) {
        PostCommentValidator.validateContent(content);

        PostComment reply = PostCommentValidator.getPostCommentOrThrow(replyId, queryRepository::fetchOneById);
        PostCommentValidator.validateIsReply(reply);
        PostCommentValidator.validateOwnership(reply, userId);

        reply.updateContent(content);
        return PostCommentDto.from(reply);
    }

    /**
     * 8. 대댓글 삭제. 작성자 본인만 가능, 대상은 2-depth 대댓글이어야 한다. soft-delete 한다.
     */
    public void deleteReply(UUID userId, UUID replyId) {
        PostComment reply = PostCommentValidator.getPostCommentOrThrow(replyId, queryRepository::fetchOneById);
        PostCommentValidator.validateIsReply(reply);
        PostCommentValidator.validateOwnership(reply, userId);

        reply.softDelete();
        postQueryRepository.decrementCommentCount(reply.post().id());
    }

    /**
     * 9. 내가 작성한 댓글/대댓글 목록 조회. 댓글, 대댓글을 구분하지 않고 createdAt 내림차순으로 반환한다. 응답 스키마 일관성을 위해 has_child_comment 도 함께 계산한다.
     */
    @Transactional(readOnly = true)
    public OffsetPageResult<PostCommentDto> getMyComments(UUID userId, Pageable pageable) {
        PostCommentValidator.validateUserId(userId);

        OffsetPageResult<PostComment> page = queryRepository.fetchByUserId(userId, pageable);
        return mapWithHasChildComment(page);
    }

    /**
     * 페이지 결과의 각 댓글에 대해 자식 댓글(대댓글, soft-delete 포함) 존재 여부를 1회의 추가 쿼리로 일괄 조회하여 PostCommentDto로 매핑한다.
     * 삭제된 자식이 있어도 has_child_comment 를 true 로 유지해 하위 replies 조회가 가능하도록 한다. 페이지가 비어 있으면 추가 쿼리를 생략한다.
     */
    private OffsetPageResult<PostCommentDto> mapWithHasChildComment(OffsetPageResult<PostComment> page) {
        List<UUID> ids = page.getItems().stream()
                             .map(PostComment::id)
                             .toList();

        Set<UUID> parentIdsHavingChildren = queryRepository.findParentIdsHavingChildren(ids);

        return page.map(comment -> PostCommentDto.from(comment, parentIdsHavingChildren.contains(comment.id())));
    }
}
