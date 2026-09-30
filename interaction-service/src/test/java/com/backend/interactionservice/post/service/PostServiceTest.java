package com.backend.interactionservice.post.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.interactionservice.post.repository.PostDocumentElasticsearchRepository;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.repository.query.SearchCondition;
import com.backend.interactionservice.post.service.dto.PostDocument;
import com.backend.interactionservice.post.service.dto.PostListItem;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@DisplayName("PostService 테스트")
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @Spy
    @InjectMocks
    private PostService postService;

    @Mock
    private PostDocumentElasticsearchRepository postDocumentElasticsearchRepository;

    @Mock
    private PostQueryRepository postQueryRepository;

    private Post mockPost;
    private PostDocument mockDocument1;
    private PostDocument mockDocument2;

    @BeforeEach
    void init() {
        mockPost = Post.builder()
                       .id(UUID.randomUUID())
                       .postStatus(PostStatus.ACTIVE)
                       .build();

        mockDocument1 = createDocument(UUID.randomUUID(), "title1");
        mockDocument2 = createDocument(UUID.randomUUID(), "title2");
    }

    private PostDocument createDocument(UUID id, String title) {
        return new PostDocument(id,
                                title,
                                "https://test.com/" + id,
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
    }

    @DisplayName("Post 조회 테스트")
    @Nested
    class GetPostTest {

        @Test
        void id를_입력하면_Post를_조회할_수_있다() {
            // given
            Mockito.doReturn(Optional.of(mockPost)).when(postQueryRepository).fetchOneById(any());

            // when
            Post result = postService.getPost(mockPost.id());

            // then
            Assertions.assertThat(result).isSameAs(mockPost);
        }

        @Test
        void id가_null이면_Post_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postService.getPost(null))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }

        @Test
        void 존재하지_않는_id로_조회하면_Post_조회에_실패한다() {
            // given
            Mockito.doReturn(Optional.empty()).when(postQueryRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> postService.getPost(UUID.randomUUID()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 post입니다.");
        }
    }

    @DisplayName("Post 목록 검색 테스트")
    @Nested
    class SearchPostsTest {

        @Test
        void 검색_결과에_내_좋아요_북마크_여부를_함께_반환한다() {
            // given
            UUID userId = UUID.randomUUID();
            SearchCondition condition = SearchCondition.builder().title("spring").build();
            Pageable pageable = PageRequest.of(0, 20);
            OffsetPageResult<PostDocument> esResult = new OffsetPageResult<>(2L, 0, 20, List.of(mockDocument1, mockDocument2));

            Mockito.doReturn(esResult).when(postDocumentElasticsearchRepository).search(condition, pageable);
            Mockito.doReturn(Set.of(mockDocument1.id())).when(postQueryRepository).findLikedPostIds(eq(userId), anyCollection());
            Mockito.doReturn(Set.of(mockDocument2.id())).when(postQueryRepository).findBookmarkedPostIds(eq(userId), anyCollection());

            // when
            OffsetPageResult<PostListItem> result = postService.searchPosts(condition, userId, pageable);

            // then
            Assertions.assertThat(result.getTotalCount()).isEqualTo(2L);
            Assertions.assertThat(result.getItems()).hasSize(2);
            Assertions.assertThat(result.getItems().get(0).id()).isEqualTo(mockDocument1.id());
            Assertions.assertThat(result.getItems().get(0).likesOfMe()).isTrue();
            Assertions.assertThat(result.getItems().get(0).bookmarksOfMe()).isFalse();
            Assertions.assertThat(result.getItems().get(1).likesOfMe()).isFalse();
            Assertions.assertThat(result.getItems().get(1).bookmarksOfMe()).isTrue();
        }

        @Test
        void 검색_결과가_비어있으면_추가_조회없이_빈_목록을_반환한다() {
            // given
            SearchCondition condition = SearchCondition.builder().build();
            Pageable pageable = PageRequest.of(1, 10);
            OffsetPageResult<PostDocument> esResult = new OffsetPageResult<>(0L, 1, 10, List.of());

            Mockito.doReturn(esResult).when(postDocumentElasticsearchRepository).search(condition, pageable);

            // when
            OffsetPageResult<PostListItem> result = postService.searchPosts(condition, null, pageable);

            // then
            Assertions.assertThat(result.getTotalCount()).isZero();
            Assertions.assertThat(result.getPage()).isEqualTo(1);
            Assertions.assertThat(result.getSize()).isEqualTo(10);
            Assertions.assertThat(result.getItems()).isEmpty();
            Mockito.verify(postQueryRepository, Mockito.never()).findLikedPostIds(any(), anyCollection());
            Mockito.verify(postQueryRepository, Mockito.never()).findBookmarkedPostIds(any(), anyCollection());
        }
    }

    @DisplayName("Post 단건 검색 테스트")
    @Nested
    class SearchPostTest {

        @Test
        void postId를_입력하면_내_좋아요_북마크_여부와_함께_조회할_수_있다() {
            // given
            UUID userId = UUID.randomUUID();
            Mockito.doReturn(Optional.of(mockDocument1)).when(postDocumentElasticsearchRepository).fetchOneById(mockDocument1.id());
            Mockito.doReturn(Set.of(mockDocument1.id())).when(postQueryRepository).findLikedPostIds(eq(userId), anyCollection());
            Mockito.doReturn(Set.of()).when(postQueryRepository).findBookmarkedPostIds(eq(userId), anyCollection());

            // when
            PostListItem result = postService.searchPost(mockDocument1.id(), userId);

            // then
            Assertions.assertThat(result.id()).isEqualTo(mockDocument1.id());
            Assertions.assertThat(result.title()).isEqualTo(mockDocument1.title());
            Assertions.assertThat(result.likesOfMe()).isTrue();
            Assertions.assertThat(result.bookmarksOfMe()).isFalse();
        }

        @Test
        void postId가_null이면_조회에_실패한다() {
            // when & then
            Assertions.assertThatThrownBy(() -> postService.searchPost(null, UUID.randomUUID()))
                      .isInstanceOf(BadRequestException.class)
                      .hasMessageContaining("id는 필수 입력값입니다.");
        }

        @Test
        void 존재하지_않는_postId로_조회하면_조회에_실패한다() {
            // given
            Mockito.doReturn(Optional.empty()).when(postDocumentElasticsearchRepository).fetchOneById(any());

            // when & then
            Assertions.assertThatThrownBy(() -> postService.searchPost(UUID.randomUUID(), UUID.randomUUID()))
                      .isInstanceOf(NotFoundException.class)
                      .hasMessageContaining("존재하지 않는 PostDocument입니다.");
        }
    }
}
