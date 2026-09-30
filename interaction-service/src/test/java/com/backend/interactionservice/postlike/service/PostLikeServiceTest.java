package com.backend.interactionservice.postlike.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.post.PostLike;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.service.PostService;
import com.backend.interactionservice.postlike.repository.PostLikeQueryRepository;
import com.backend.interactionservice.postlike.repository.PostLikeRepository;
import com.backend.interactionservice.user.service.UserService;
import java.util.Optional;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@DisplayName("PostLikeService 테스트")
@ExtendWith(MockitoExtension.class)
class PostLikeServiceTest {

    @Spy
    @InjectMocks
    private PostLikeService postLikeService;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostLikeQueryRepository queryRepository;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostService postService;

    @Mock
    private UserService userService;

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

    private PostLike createPostLike(boolean isEnable) {
        return PostLike.builder()
                       .id(UUID.randomUUID())
                       .user(mockUser)
                       .post(mockPost)
                       .isEnable(isEnable)
                       .build();
    }

    @DisplayName("좋아요 테스트")
    @Nested
    class LikeTest {

        @Test
        void 좋아요_이력이_없으면_새로_저장하고_like_count를_증가시킨다() {
            // given
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());
            Mockito.doReturn(mockUser).when(userService).getUser(mockUser.id());
            Mockito.doReturn(mockPost).when(postService).getPost(mockPost.id());

            // when
            postLikeService.like(mockUser.id(), mockPost.id());

            // then
            ArgumentCaptor<PostLike> captor = ArgumentCaptor.forClass(PostLike.class);
            Mockito.verify(postLikeRepository).save(captor.capture());
            Assertions.assertThat(captor.getValue().user()).isSameAs(mockUser);
            Assertions.assertThat(captor.getValue().post()).isSameAs(mockPost);
            Assertions.assertThat(captor.getValue().isEnable()).isTrue();
            Mockito.verify(postQueryRepository).incrementLikeCount(mockPost.id());
        }

        @Test
        void 비활성_좋아요가_있으면_활성화하고_like_count를_증가시킨다() {
            // given
            PostLike postLike = createPostLike(false);
            Mockito.doReturn(Optional.of(postLike)).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());

            // when
            postLikeService.like(mockUser.id(), mockPost.id());

            // then
            Assertions.assertThat(postLike.isEnable()).isTrue();
            Mockito.verify(postQueryRepository).incrementLikeCount(mockPost.id());
            Mockito.verify(postLikeRepository, Mockito.never()).save(any());
        }

        @Test
        void 이미_좋아요_상태면_아무것도_하지_않는다() {
            // given
            PostLike postLike = createPostLike(true);
            Mockito.doReturn(Optional.of(postLike)).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());

            // when
            postLikeService.like(mockUser.id(), mockPost.id());

            // then
            Assertions.assertThat(postLike.isEnable()).isTrue();
            Mockito.verify(postQueryRepository, Mockito.never()).incrementLikeCount(any());
            Mockito.verify(postLikeRepository, Mockito.never()).save(any());
        }

        @Test
        void 동시_요청으로_unique_충돌이_나면_멱등하게_무시한다() {
            // given
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());
            Mockito.doReturn(mockUser).when(userService).getUser(mockUser.id());
            Mockito.doReturn(mockPost).when(postService).getPost(mockPost.id());
            Mockito.doThrow(new DataIntegrityViolationException("duplicate")).when(postLikeRepository).save(any());

            // when & then
            Assertions.assertThatCode(() -> postLikeService.like(mockUser.id(), mockPost.id()))
                      .doesNotThrowAnyException();
            Mockito.verify(postQueryRepository, Mockito.never()).incrementLikeCount(any());
        }

        @Test
        void userId가_null이면_좋아요에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postLikeService.like(null, mockPost.id()))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }

        @Test
        void postId가_null이면_좋아요에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postLikeService.like(mockUser.id(), null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("postId는 필수 입력값입니다.");
        }
    }

    @DisplayName("좋아요 취소 테스트")
    @Nested
    class UnLikeTest {

        @Test
        void 활성_좋아요가_있으면_비활성화하고_like_count를_감소시킨다() {
            // given
            PostLike postLike = createPostLike(true);
            Mockito.doReturn(Optional.of(postLike)).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());

            // when
            postLikeService.unLike(mockUser.id(), mockPost.id());

            // then
            Assertions.assertThat(postLike.isEnable()).isFalse();
            Mockito.verify(postQueryRepository).decrementLikeCount(mockPost.id());
        }

        @Test
        void 이미_비활성_좋아요면_아무것도_하지_않는다() {
            // given
            PostLike postLike = createPostLike(false);
            Mockito.doReturn(Optional.of(postLike)).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());

            // when
            postLikeService.unLike(mockUser.id(), mockPost.id());

            // then
            Assertions.assertThat(postLike.isEnable()).isFalse();
            Mockito.verify(postQueryRepository, Mockito.never()).decrementLikeCount(any());
        }

        @Test
        void 좋아요_이력이_없으면_아무것도_하지_않는다() {
            // given
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());

            // when
            postLikeService.unLike(mockUser.id(), mockPost.id());

            // then
            Mockito.verify(postQueryRepository, Mockito.never()).decrementLikeCount(any());
        }

        @Test
        void userId가_null이면_좋아요_취소에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postLikeService.unLike(null, mockPost.id()))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }
    }
}
