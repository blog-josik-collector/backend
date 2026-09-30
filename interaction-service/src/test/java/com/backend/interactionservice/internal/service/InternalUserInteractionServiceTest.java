package com.backend.interactionservice.internal.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.persistence.common.enums.PostCommentStatus;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.post.PostBookmark;
import com.backend.commondataaccess.persistence.post.PostComment;
import com.backend.commondataaccess.persistence.post.PostLike;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.postbookmark.repository.PostBookmarkQueryRepository;
import com.backend.interactionservice.postcomment.repository.PostCommentQueryRepository;
import com.backend.interactionservice.postlike.repository.PostLikeQueryRepository;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("InternalUserInteractionService 테스트")
@ExtendWith(MockitoExtension.class)
class InternalUserInteractionServiceTest {

    @InjectMocks
    private InternalUserInteractionService internalUserInteractionService;

    @Mock
    private PostCommentQueryRepository postCommentQueryRepository;

    @Mock
    private PostLikeQueryRepository postLikeQueryRepository;

    @Mock
    private PostBookmarkQueryRepository postBookmarkQueryRepository;

    @Mock
    private PostQueryRepository postQueryRepository;

    private User mockUser;
    private Post mockPost;

    @BeforeEach
    void init() {
        mockUser = User.builder()
                       .id(UUID.randomUUID())
                       .userType(UserType.USER)
                       .nickname("test_nickname")
                       .build();

        mockPost = Post.builder()
                       .id(UUID.randomUUID())
                       .postStatus(PostStatus.ACTIVE)
                       .build();
    }

    @DisplayName("softDeleteByUserId 테스트")
    @Nested
    class SoftDeleteByUserIdTest {

        @Test
        void 유저의_댓글_좋아요_즐겨찾기를_모두_soft_delete한다() {
            // given
            PostComment comment = PostComment.builder()
                                             .id(UUID.randomUUID())
                                             .user(mockUser)
                                             .post(mockPost)
                                             .content("comment")
                                             .postCommentStatus(PostCommentStatus.ACTIVE)
                                             .build();
            PostLike enabledLike = PostLike.builder().id(UUID.randomUUID()).user(mockUser).post(mockPost).isEnable(true).build();
            PostLike disabledLike = PostLike.builder().id(UUID.randomUUID()).user(mockUser).post(mockPost).isEnable(false).build();
            PostBookmark enabledBookmark = PostBookmark.builder().id(UUID.randomUUID()).user(mockUser).post(mockPost).isEnable(true).build();
            PostBookmark disabledBookmark = PostBookmark.builder().id(UUID.randomUUID()).user(mockUser).post(mockPost).isEnable(false).build();

            Mockito.doReturn(List.of(comment)).when(postCommentQueryRepository).fetchAllActiveByUserId(mockUser.id());
            Mockito.doReturn(List.of(enabledLike, disabledLike)).when(postLikeQueryRepository).fetchAllActiveByUserId(mockUser.id());
            Mockito.doReturn(List.of(enabledBookmark, disabledBookmark)).when(postBookmarkQueryRepository).fetchAllActiveByUserId(mockUser.id());

            // when
            internalUserInteractionService.softDeleteByUserId(mockUser.id());

            // then
            Assertions.assertThat(comment.postCommentStatus()).isEqualTo(PostCommentStatus.DELETED);
            Assertions.assertThat(comment.isDelete()).isTrue();
            Mockito.verify(postQueryRepository).decrementCommentCount(mockPost.id());

            Assertions.assertThat(enabledLike.isEnable()).isFalse();
            Assertions.assertThat(enabledLike.isDelete()).isTrue();
            Assertions.assertThat(disabledLike.isDelete()).isTrue();
            // 활성 좋아요 1건에 대해서만 like_count 를 감소시킨다.
            Mockito.verify(postQueryRepository, Mockito.times(1)).decrementLikeCount(mockPost.id());

            Assertions.assertThat(enabledBookmark.isEnable()).isFalse();
            Assertions.assertThat(enabledBookmark.isDelete()).isTrue();
            Assertions.assertThat(disabledBookmark.isDelete()).isTrue();
        }

        @Test
        void 삭제할_리소스가_없으면_아무것도_하지_않는다() {
            // given
            Mockito.doReturn(List.of()).when(postCommentQueryRepository).fetchAllActiveByUserId(mockUser.id());
            Mockito.doReturn(List.of()).when(postLikeQueryRepository).fetchAllActiveByUserId(mockUser.id());
            Mockito.doReturn(List.of()).when(postBookmarkQueryRepository).fetchAllActiveByUserId(mockUser.id());

            // when
            internalUserInteractionService.softDeleteByUserId(mockUser.id());

            // then
            Mockito.verify(postQueryRepository, Mockito.never()).decrementCommentCount(any());
            Mockito.verify(postQueryRepository, Mockito.never()).decrementLikeCount(any());
        }

        @Test
        void userId가_null이면_IllegalArgumentException을_던진다() {
            // when & then
            Assertions.assertThatThrownBy(() -> internalUserInteractionService.softDeleteByUserId(null))
                      .isInstanceOf(IllegalArgumentException.class)
                      .hasMessageContaining("userId must not be null");
        }
    }
}
