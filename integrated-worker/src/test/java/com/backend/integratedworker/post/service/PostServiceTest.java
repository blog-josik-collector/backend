package com.backend.integratedworker.post.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.integratedworker.post.repository.PostRepository;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PostService 테스트")
@ExtendWith(MockitoExtension.class)
class PostServiceTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @DisplayName("createPostsIfAbsent 테스트")
    @Nested
    class CreatePostsIfAbsentTest {

        @Test
        void 빈_id_리스트가_입력되면_saveAll은_빈_리스트로_호출된다() {
            Mockito.doReturn(List.of()).when(postRepository).findAllById(any());

            postService.createPostsIfAbsent(List.of());

            Mockito.verify(postRepository).saveAll(Collections.emptyList());
        }

        @Test
        void 모두_신규인_id이면_모든_id가_ACTIVE_상태로_insert된다() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();

            Mockito.doReturn(List.of()).when(postRepository).findAllById(any());

            postService.createPostsIfAbsent(List.of(id1, id2));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Post>> captor = ArgumentCaptor.forClass(List.class);
            Mockito.verify(postRepository).saveAll(captor.capture());

            List<Post> saved = captor.getValue();
            Assertions.assertThat(saved).hasSize(2);
            Assertions.assertThat(saved.stream().map(Post::id))
                      .containsExactlyInAnyOrder(id1, id2);
            Assertions.assertThat(saved)
                      .allSatisfy(p -> {
                          Assertions.assertThat(p.postStatus()).isEqualTo(PostStatus.ACTIVE);
                          Assertions.assertThat(p.likeCount()).isZero();
                          Assertions.assertThat(p.viewCount()).isZero();
                          Assertions.assertThat(p.commentCount()).isZero();
                          Assertions.assertThat(p.totalReportCount()).isZero();
                      });
        }

        @Test
        void 모두_존재하는_id이면_saveAll이_빈_리스트로_호출된다() {
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Post existing1 = Post.builder().id(id1).postStatus(PostStatus.ACTIVE).build();
            Post existing2 = Post.builder().id(id2).postStatus(PostStatus.ACTIVE).build();

            Mockito.doReturn(List.of(existing1, existing2)).when(postRepository).findAllById(any());

            postService.createPostsIfAbsent(List.of(id1, id2));

            Mockito.verify(postRepository).saveAll(Collections.emptyList());
        }

        @Test
        void 일부만_존재하면_없는_id만_insert된다() {
            UUID existingId = UUID.randomUUID();
            UUID newId = UUID.randomUUID();
            Post existing = Post.builder().id(existingId).postStatus(PostStatus.ACTIVE).build();

            Mockito.doReturn(List.of(existing)).when(postRepository).findAllById(any());

            postService.createPostsIfAbsent(List.of(existingId, newId));

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Post>> captor = ArgumentCaptor.forClass(List.class);
            Mockito.verify(postRepository).saveAll(captor.capture());

            List<Post> saved = captor.getValue();
            Assertions.assertThat(saved).hasSize(1);
            Assertions.assertThat(saved.get(0).id()).isEqualTo(newId);
        }

        @Test
        void 입력_리스트가_불변_리스트여도_예외없이_동작한다() {
            // Stream#toList()로 만들어진 immutable list를 받아도 호출자의 리스트를 변경하지 않아야 한다.
            // (옵션B에서 IndexingService가 targetIds를 Stream#toList()로 만들어 전달하기 때문에 중요)
            List<UUID> immutable = List.of(UUID.randomUUID(), UUID.randomUUID());

            Mockito.doReturn(List.of()).when(postRepository).findAllById(any());

            Assertions.assertThatCode(() -> postService.createPostsIfAbsent(immutable))
                      .doesNotThrowAnyException();

            Assertions.assertThat(immutable).hasSize(2);
        }

        @Test
        void 같은_id로_여러번_호출해도_이미_있는_id는_건너뛴다_멱등성() {
            // 옵션B의 핵심: 재시도 시 멱등성. 첫 번째 호출 후 같은 id로 다시 호출해도 추가 insert 없음.
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            Post existing1 = Post.builder().id(id1).postStatus(PostStatus.ACTIVE).build();
            Post existing2 = Post.builder().id(id2).postStatus(PostStatus.ACTIVE).build();

            // 1차 호출: 모두 신규
            Mockito.doReturn(List.of()).when(postRepository).findAllById(any());
            postService.createPostsIfAbsent(List.of(id1, id2));

            // 2차 호출: 이미 둘 다 존재한다고 가정 → 신규 insert 없어야 함
            Mockito.reset(postRepository);
            Mockito.doReturn(List.of(existing1, existing2)).when(postRepository).findAllById(any());
            postService.createPostsIfAbsent(List.of(id1, id2));

            Mockito.verify(postRepository).saveAll(Collections.emptyList());
        }
    }
}
