package com.backend.interactionservice.postcomment.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.exception.AccessDeniedException;
import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.common.enums.PostCommentStatus;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.post.PostComment;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.service.PostService;
import com.backend.interactionservice.postcomment.repository.PostCommentQueryRepository;
import com.backend.interactionservice.postcomment.repository.PostCommentRepository;
import com.backend.interactionservice.postcomment.service.dto.PostCommentDto;
import com.backend.interactionservice.user.service.UserService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("PostCommentService 테스트")
@ExtendWith(MockitoExtension.class)
class PostCommentServiceTest {

    @Spy
    @InjectMocks
    private PostCommentService postCommentService;

    @Mock
    private PostCommentRepository postCommentRepository;

    @Mock
    private PostCommentQueryRepository queryRepository;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostService postService;

    @Mock
    private UserService userService;

    private User mockUser;
    private Post mockPost;
    private PostComment mockComment;
    private PostComment mockReply;

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

        mockComment = PostComment.builder()
                                 .id(UUID.randomUUID())
                                 .user(mockUser)
                                 .post(mockPost)
                                 .content("test_comment")
                                 .postCommentStatus(PostCommentStatus.ACTIVE)
                                 .build();

        mockReply = PostComment.builder()
                               .id(UUID.randomUUID())
                               .user(mockUser)
                               .post(mockPost)
                               .parentComment(mockComment)
                               .content("test_reply")
                               .postCommentStatus(PostCommentStatus.ACTIVE)
                               .build();
    }

    @DisplayName("댓글 단건 조회 테스트")
    @Nested
    class GetCommentTest {

        @Test
        void commentId를_입력하면_댓글을_조회할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockComment)).when(queryRepository).fetchOneById(mockComment.id());

            // when
            PostComment result = postCommentService.getComment(mockComment.id());

            // then
            Assertions.assertThat(result).isSameAs(mockComment);
        }

        @Test
        void 존재하지_않는_commentId로_조회하면_실패한다() {
            // given
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.getComment(UUID.randomUUID()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 댓글입니다.");
        }
    }

    @DisplayName("댓글 작성 테스트")
    @Nested
    class CreateCommentTest {

        @Test
        void 댓글을_작성하면_저장하고_comment_count를_증가시킨다() {
            // given
            Mockito.doReturn(mockUser).when(userService).getUser(mockUser.id());
            Mockito.doReturn(mockPost).when(postService).getPost(mockPost.id());
            Mockito.doReturn(mockComment).when(postCommentRepository).save(any(PostComment.class));

            // when
            PostCommentDto result = postCommentService.createComment(mockUser.id(), mockPost.id(), "test_comment");

            // then
            ArgumentCaptor<PostComment> captor = ArgumentCaptor.forClass(PostComment.class);
            Mockito.verify(postCommentRepository).save(captor.capture());
            Assertions.assertThat(captor.getValue().parentComment()).isNull();
            Assertions.assertThat(captor.getValue().content()).isEqualTo("test_comment");
            Assertions.assertThat(captor.getValue().postCommentStatus()).isEqualTo(PostCommentStatus.ACTIVE);
            Mockito.verify(postQueryRepository).incrementCommentCount(mockPost.id());

            Assertions.assertThat(result.id()).isEqualTo(mockComment.id());
            Assertions.assertThat(result.postId()).isEqualTo(mockPost.id());
            Assertions.assertThat(result.userId()).isEqualTo(mockUser.id());
            Assertions.assertThat(result.nickname()).isEqualTo(mockUser.nickname());
            Assertions.assertThat(result.reply()).isFalse();
            Assertions.assertThat(result.hasChildComment()).isFalse();
        }

        @Test
        void userId가_null이면_댓글_작성에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.createComment(null, mockPost.id(), "content"))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }

        @Test
        void postId가_null이면_댓글_작성에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.createComment(mockUser.id(), null, "content"))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("postId는 필수 입력값입니다.");
        }

        @Test
        void content가_비어있으면_댓글_작성에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.createComment(mockUser.id(), mockPost.id(), " "))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("댓글 내용은 비어있을 수 없습니다.");
        }
    }

    @DisplayName("댓글 목록 조회 테스트")
    @Nested
    class GetCommentsTest {

        @Test
        void postId를_입력하면_대댓글_보유_여부와_함께_댓글_목록을_조회할_수_있다() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            PostComment otherComment = PostComment.builder()
                                                  .id(UUID.randomUUID())
                                                  .user(mockUser)
                                                  .post(mockPost)
                                                  .content("other")
                                                  .postCommentStatus(PostCommentStatus.ACTIVE)
                                                  .build();
            OffsetPageResult<PostComment> page = new OffsetPageResult<>(2L, 0, 20, List.of(mockComment, otherComment));

            Mockito.doReturn(page).when(queryRepository).fetchCommentsByPostId(mockPost.id(), pageable);
            Mockito.doReturn(Set.of(mockComment.id())).when(queryRepository).findParentIdsHavingChildren(anyCollection());

            // when
            OffsetPageResult<PostCommentDto> result = postCommentService.getComments(mockPost.id(), pageable);

            // then
            Assertions.assertThat(result.getTotalCount()).isEqualTo(2L);
            Assertions.assertThat(result.getItems()).hasSize(2);
            Assertions.assertThat(result.getItems().get(0).hasChildComment()).isTrue();
            Assertions.assertThat(result.getItems().get(1).hasChildComment()).isFalse();
        }

        @Test
        void postId가_null이면_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.getComments(null, PageRequest.of(0, 20)))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("postId는 필수 입력값입니다.");
        }
    }

    @DisplayName("댓글 수정 테스트")
    @Nested
    class UpdateCommentTest {

        @Test
        void 작성자_본인이면_댓글을_수정할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockComment)).when(queryRepository).fetchOneById(mockComment.id());

            // when
            PostCommentDto result = postCommentService.updateComment(mockUser.id(), mockComment.id(), "updated");

            // then
            Assertions.assertThat(mockComment.content()).isEqualTo("updated");
            Assertions.assertThat(result.content()).isEqualTo("updated");
        }

        @Test
        void 작성자가_아니면_댓글_수정에_실패한다() {
            // given
            Mockito.doReturn(Optional.of(mockComment)).when(queryRepository).fetchOneById(mockComment.id());

            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.updateComment(UUID.randomUUID(), mockComment.id(), "updated"))
                      .isInstanceOf(AccessDeniedException.class)
                      .hasMessageContaining("댓글에 대한 권한이 없습니다.");
        }

        @Test
        void 대상이_대댓글이면_댓글_수정에_실패한다() {
            // given
            Mockito.doReturn(Optional.of(mockReply)).when(queryRepository).fetchOneById(mockReply.id());

            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.updateComment(mockUser.id(), mockReply.id(), "updated"))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("해당 리소스는 대댓글입니다.");
        }
    }

    @DisplayName("댓글 삭제 테스트")
    @Nested
    class DeleteCommentTest {

        @Test
        void 작성자_본인이면_댓글을_soft_delete하고_comment_count를_감소시킨다() {
            // given
            Mockito.doReturn(Optional.of(mockComment)).when(queryRepository).fetchOneById(mockComment.id());

            // when
            postCommentService.deleteComment(mockUser.id(), mockComment.id());

            // then
            Assertions.assertThat(mockComment.postCommentStatus()).isEqualTo(PostCommentStatus.DELETED);
            Assertions.assertThat(mockComment.isDelete()).isTrue();
            Mockito.verify(postQueryRepository).decrementCommentCount(mockPost.id());
        }

        @Test
        void 작성자가_아니면_댓글_삭제에_실패한다() {
            // given
            Mockito.doReturn(Optional.of(mockComment)).when(queryRepository).fetchOneById(mockComment.id());

            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.deleteComment(UUID.randomUUID(), mockComment.id()))
                      .isInstanceOf(AccessDeniedException.class);
            Mockito.verify(postQueryRepository, Mockito.never()).decrementCommentCount(any());
        }
    }

    @DisplayName("대댓글 작성 테스트")
    @Nested
    class CreateReplyTest {

        @Test
        void 대댓글을_작성하면_저장하고_comment_count를_증가시킨다() {
            // given
            Mockito.doReturn(Optional.of(mockComment)).when(queryRepository).fetchOneById(mockComment.id());
            Mockito.doReturn(mockUser).when(userService).getUser(mockUser.id());
            Mockito.doReturn(mockReply).when(postCommentRepository).save(any(PostComment.class));

            // when
            PostCommentDto result = postCommentService.createReply(mockUser.id(), mockComment.id(), "test_reply");

            // then
            ArgumentCaptor<PostComment> captor = ArgumentCaptor.forClass(PostComment.class);
            Mockito.verify(postCommentRepository).save(captor.capture());
            Assertions.assertThat(captor.getValue().parentComment()).isSameAs(mockComment);
            Assertions.assertThat(captor.getValue().post()).isSameAs(mockPost);
            Mockito.verify(postQueryRepository).incrementCommentCount(mockPost.id());

            Assertions.assertThat(result.id()).isEqualTo(mockReply.id());
            Assertions.assertThat(result.parentCommentId()).isEqualTo(mockComment.id());
            Assertions.assertThat(result.reply()).isTrue();
        }

        @Test
        void 부모_댓글이_없으면_대댓글_작성에_실패한다() {
            // given
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.createReply(mockUser.id(), UUID.randomUUID(), "reply"))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 댓글입니다.");
        }

        @Test
        void userId가_null이면_대댓글_작성에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.createReply(null, mockComment.id(), "reply"))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }
    }

    @DisplayName("대댓글 목록 조회 테스트")
    @Nested
    class GetRepliesTest {

        @Test
        void 부모_댓글_id를_입력하면_대댓글_목록을_조회할_수_있다() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            OffsetPageResult<PostComment> page = new OffsetPageResult<>(1L, 0, 20, List.of(mockReply));

            Mockito.doReturn(page).when(queryRepository).fetchRepliesByParentId(mockComment.id(), pageable);
            Mockito.doReturn(Set.of()).when(queryRepository).findParentIdsHavingChildren(anyCollection());

            // when
            OffsetPageResult<PostCommentDto> result = postCommentService.getReplies(mockComment.id(), pageable);

            // then
            Assertions.assertThat(result.getItems()).hasSize(1);
            Assertions.assertThat(result.getItems().get(0).id()).isEqualTo(mockReply.id());
            Assertions.assertThat(result.getItems().get(0).reply()).isTrue();
            Assertions.assertThat(result.getItems().get(0).hasChildComment()).isFalse();
        }

        @Test
        void 부모_댓글_id가_null이면_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.getReplies(null, PageRequest.of(0, 20)))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("commentId는 필수 입력값입니다.");
        }
    }

    @DisplayName("대댓글 수정 테스트")
    @Nested
    class UpdateReplyTest {

        @Test
        void 작성자_본인이면_대댓글을_수정할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockReply)).when(queryRepository).fetchOneById(mockReply.id());

            // when
            PostCommentDto result = postCommentService.updateReply(mockUser.id(), mockReply.id(), "updated_reply");

            // then
            Assertions.assertThat(mockReply.content()).isEqualTo("updated_reply");
            Assertions.assertThat(result.content()).isEqualTo("updated_reply");
        }

        @Test
        void 대상이_댓글이면_대댓글_수정에_실패한다() {
            // given
            Mockito.doReturn(Optional.of(mockComment)).when(queryRepository).fetchOneById(mockComment.id());

            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.updateReply(mockUser.id(), mockComment.id(), "updated"))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("해당 리소스는 댓글입니다.");
        }
    }

    @DisplayName("대댓글 삭제 테스트")
    @Nested
    class DeleteReplyTest {

        @Test
        void 작성자_본인이면_대댓글을_soft_delete하고_comment_count를_감소시킨다() {
            // given
            Mockito.doReturn(Optional.of(mockReply)).when(queryRepository).fetchOneById(mockReply.id());

            // when
            postCommentService.deleteReply(mockUser.id(), mockReply.id());

            // then
            Assertions.assertThat(mockReply.postCommentStatus()).isEqualTo(PostCommentStatus.DELETED);
            Assertions.assertThat(mockReply.isDelete()).isTrue();
            Mockito.verify(postQueryRepository).decrementCommentCount(mockPost.id());
        }

        @Test
        void 대상이_댓글이면_대댓글_삭제에_실패한다() {
            // given
            Mockito.doReturn(Optional.of(mockComment)).when(queryRepository).fetchOneById(mockComment.id());

            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.deleteReply(mockUser.id(), mockComment.id()))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("해당 리소스는 댓글입니다.");
        }
    }

    @DisplayName("내 댓글 목록 조회 테스트")
    @Nested
    class GetMyCommentsTest {

        @Test
        void userId를_입력하면_내가_작성한_댓글과_대댓글을_조회할_수_있다() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            OffsetPageResult<PostComment> page = new OffsetPageResult<>(2L, 0, 20, List.of(mockReply, mockComment));

            Mockito.doReturn(page).when(queryRepository).fetchByUserId(mockUser.id(), pageable);
            Mockito.doReturn(Set.of(mockComment.id())).when(queryRepository).findParentIdsHavingChildren(anyCollection());

            // when
            OffsetPageResult<PostCommentDto> result = postCommentService.getMyComments(mockUser.id(), pageable);

            // then
            Assertions.assertThat(result.getItems()).hasSize(2);
            Assertions.assertThat(result.getItems().get(0).reply()).isTrue();
            Assertions.assertThat(result.getItems().get(1).hasChildComment()).isTrue();
        }

        @Test
        void userId가_null이면_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postCommentService.getMyComments(null, PageRequest.of(0, 20)))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }
    }
}
