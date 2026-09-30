package com.backend.interactionservice.postbookmark.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commondataaccess.persistence.post.PostBookmark;
import com.backend.commondataaccess.persistence.user.User;
import com.backend.commondataaccess.persistence.user.enums.UserType;
import com.backend.interactionservice.post.service.PostService;
import com.backend.interactionservice.post.service.dto.PostDocument;
import com.backend.interactionservice.postbookmark.repository.PostBookmarkElasticsearchRepository;
import com.backend.interactionservice.postbookmark.repository.PostBookmarkQueryRepository;
import com.backend.interactionservice.postbookmark.repository.PostBookmarkRepository;
import com.backend.interactionservice.user.service.UserService;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("PostBookmarkService 테스트")
@ExtendWith(MockitoExtension.class)
class PostBookmarkServiceTest {

    @Spy
    @InjectMocks
    private PostBookmarkService postBookmarkService;

    @Mock
    private PostBookmarkRepository postBookmarkRepository;

    @Mock
    private PostBookmarkQueryRepository queryRepository;

    @Mock
    private PostBookmarkElasticsearchRepository postBookmarkElasticsearchRepository;

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

    private PostBookmark createPostBookmark(boolean isEnable) {
        return PostBookmark.builder()
                           .id(UUID.randomUUID())
                           .user(mockUser)
                           .post(mockPost)
                           .isEnable(isEnable)
                           .build();
    }

    @DisplayName("즐겨찾기 테스트")
    @Nested
    class BookmarkTest {

        @Test
        void 즐겨찾기_이력이_없으면_새로_저장한다() {
            // given
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());
            Mockito.doReturn(mockUser).when(userService).getUser(mockUser.id());
            Mockito.doReturn(mockPost).when(postService).getPost(mockPost.id());

            // when
            postBookmarkService.bookmark(mockUser.id(), mockPost.id());

            // then
            ArgumentCaptor<PostBookmark> captor = ArgumentCaptor.forClass(PostBookmark.class);
            Mockito.verify(postBookmarkRepository).save(captor.capture());
            Assertions.assertThat(captor.getValue().user()).isSameAs(mockUser);
            Assertions.assertThat(captor.getValue().post()).isSameAs(mockPost);
            Assertions.assertThat(captor.getValue().isEnable()).isTrue();
        }

        @Test
        void 비활성_즐겨찾기가_있으면_활성화한다() {
            // given
            PostBookmark postBookmark = createPostBookmark(false);
            Mockito.doReturn(Optional.of(postBookmark)).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());

            // when
            postBookmarkService.bookmark(mockUser.id(), mockPost.id());

            // then
            Assertions.assertThat(postBookmark.isEnable()).isTrue();
            Mockito.verify(postBookmarkRepository, Mockito.never()).save(any());
        }

        @Test
        void 이미_즐겨찾기_상태면_아무것도_하지_않는다() {
            // given
            PostBookmark postBookmark = createPostBookmark(true);
            Mockito.doReturn(Optional.of(postBookmark)).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());

            // when
            postBookmarkService.bookmark(mockUser.id(), mockPost.id());

            // then
            Assertions.assertThat(postBookmark.isEnable()).isTrue();
            Mockito.verify(postBookmarkRepository, Mockito.never()).save(any());
        }

        @Test
        void 동시_요청으로_unique_충돌이_나면_멱등하게_무시한다() {
            // given
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());
            Mockito.doReturn(mockUser).when(userService).getUser(mockUser.id());
            Mockito.doReturn(mockPost).when(postService).getPost(mockPost.id());
            Mockito.doThrow(new DataIntegrityViolationException("duplicate")).when(postBookmarkRepository).save(any());

            // when & then
            Assertions.assertThatCode(() -> postBookmarkService.bookmark(mockUser.id(), mockPost.id()))
                      .doesNotThrowAnyException();
        }

        @Test
        void userId가_null이면_즐겨찾기에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postBookmarkService.bookmark(null, mockPost.id()))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }

        @Test
        void postId가_null이면_즐겨찾기에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postBookmarkService.bookmark(mockUser.id(), null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("postId는 필수 입력값입니다.");
        }
    }

    @DisplayName("즐겨찾기 취소 테스트")
    @Nested
    class UnBookmarkTest {

        @Test
        void 활성_즐겨찾기가_있으면_비활성화한다() {
            // given
            PostBookmark postBookmark = createPostBookmark(true);
            Mockito.doReturn(Optional.of(postBookmark)).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());

            // when
            postBookmarkService.unBookmark(mockUser.id(), mockPost.id());

            // then
            Assertions.assertThat(postBookmark.isEnable()).isFalse();
        }

        @Test
        void 이미_비활성_즐겨찾기면_아무것도_하지_않는다() {
            // given
            PostBookmark postBookmark = createPostBookmark(false);
            Mockito.doReturn(Optional.of(postBookmark)).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());

            // when
            postBookmarkService.unBookmark(mockUser.id(), mockPost.id());

            // then
            Assertions.assertThat(postBookmark.isEnable()).isFalse();
        }

        @Test
        void 즐겨찾기_이력이_없으면_NotFoundException을_던진다() {
            // given
            Mockito.doReturn(Optional.empty()).when(queryRepository).fetchOneByUserAndPost(mockUser.id(), mockPost.id());

            // when & then
            Assertions.assertThatThrownBy(() -> postBookmarkService.unBookmark(mockUser.id(), mockPost.id()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 postBookmark입니다.");
        }

        @Test
        void postId가_null이면_즐겨찾기_취소에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postBookmarkService.unBookmark(mockUser.id(), null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("postId는 필수 입력값입니다.");
        }
    }

    @DisplayName("내 즐겨찾기 목록 조회 테스트")
    @Nested
    class GetMyBookmarksTest {

        @Test
        void 즐겨찾기한_post_id로_ES에서_PostDocument를_조회한다() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            UUID postId = mockPost.id();
            OffsetPageResult<UUID> bookmarkPage = new OffsetPageResult<>(1L, 0, 20, List.of(postId));
            PostDocument document = new PostDocument(postId,
                                                     "title",
                                                     "https://test.com/1",
                                                     null,
                                                     "summary",
                                                     "Toss",
                                                     PostStatus.ACTIVE,
                                                     LocalDate.now(),
                                                     OffsetDateTime.now(),
                                                     OffsetDateTime.now(),
                                                     1,
                                                     2,
                                                     3,
                                                     0);

            Mockito.doReturn(bookmarkPage).when(queryRepository).fetchActivePostIdsByUserId(mockUser.id(), pageable);
            Mockito.doReturn(List.of(document)).when(postBookmarkElasticsearchRepository).findByIdsInOrder(List.of(postId));

            // when
            OffsetPageResult<PostDocument> result = postBookmarkService.getMyBookmarks(mockUser.id(), pageable);

            // then
            Assertions.assertThat(result.getTotalCount()).isEqualTo(1L);
            Assertions.assertThat(result.getItems()).containsExactly(document);
        }

        @Test
        void 즐겨찾기가_없으면_ES_조회없이_빈_목록을_반환한다() {
            // given
            Pageable pageable = PageRequest.of(0, 20);
            OffsetPageResult<UUID> bookmarkPage = new OffsetPageResult<>(0L, 0, 20, List.of());

            Mockito.doReturn(bookmarkPage).when(queryRepository).fetchActivePostIdsByUserId(mockUser.id(), pageable);

            // when
            OffsetPageResult<PostDocument> result = postBookmarkService.getMyBookmarks(mockUser.id(), pageable);

            // then
            Assertions.assertThat(result.getTotalCount()).isZero();
            Assertions.assertThat(result.getItems()).isEmpty();
            Mockito.verify(postBookmarkElasticsearchRepository, Mockito.never()).findByIdsInOrder(anyList());
        }

        @Test
        void userId가_null이면_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postBookmarkService.getMyBookmarks(null, PageRequest.of(0, 20)))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("userId는 필수 입력값입니다.");
        }
    }
}
