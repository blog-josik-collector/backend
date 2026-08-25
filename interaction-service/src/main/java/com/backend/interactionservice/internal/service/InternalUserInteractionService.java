package com.backend.interactionservice.internal.service;

import com.backend.commondataaccess.persistence.post.PostBookmark;
import com.backend.commondataaccess.persistence.post.PostComment;
import com.backend.commondataaccess.persistence.post.PostLike;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.postbookmark.repository.PostBookmarkQueryRepository;
import com.backend.interactionservice.postcomment.repository.PostCommentQueryRepository;
import com.backend.interactionservice.postlike.repository.PostLikeQueryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * 회원 탈퇴 시 해당 유저의 interaction 리소스를 soft-delete 한다.
 * <p>
 * 멱등하다: 이미 soft-delete 된 행은 조회 대상에서 제외된다.
 */
@Slf4j
@Transactional
@Service
@RequiredArgsConstructor
public class InternalUserInteractionService {

    private final PostCommentQueryRepository postCommentQueryRepository;
    private final PostLikeQueryRepository postLikeQueryRepository;
    private final PostBookmarkQueryRepository postBookmarkQueryRepository;
    private final PostQueryRepository postQueryRepository;

    public void softDeleteByUserId(UUID userId) {
        Assert.notNull(userId, "userId must not be null");

        int commentCount = softDeleteComments(userId);
        int likeCount = softDeleteLikes(userId);
        int bookmarkCount = softDeleteBookmarks(userId);

        log.info("[InternalUserInteraction] soft-deleted userId={} comments={} likes={} bookmarks={}",
                 userId, commentCount, likeCount, bookmarkCount);
    }

    private int softDeleteComments(UUID userId) {
        List<PostComment> comments = postCommentQueryRepository.fetchAllActiveByUserId(userId);
        for (PostComment comment : comments) {
            comment.softDelete();
            postQueryRepository.decrementCommentCount(comment.post().id());
        }
        return comments.size();
    }

    private int softDeleteLikes(UUID userId) {
        List<PostLike> likes = postLikeQueryRepository.fetchAllActiveByUserId(userId);
        for (PostLike like : likes) {
            if (like.isEnable()) {
                like.deactivate();
                postQueryRepository.decrementLikeCount(like.post().id());
            }
            like.delete();
        }
        return likes.size();
    }

    private int softDeleteBookmarks(UUID userId) {
        List<PostBookmark> bookmarks = postBookmarkQueryRepository.fetchAllActiveByUserId(userId);
        for (PostBookmark bookmark : bookmarks) {
            if (bookmark.isEnable()) {
                bookmark.deactivate();
            }
            bookmark.delete();
        }
        return bookmarks.size();
    }
}
