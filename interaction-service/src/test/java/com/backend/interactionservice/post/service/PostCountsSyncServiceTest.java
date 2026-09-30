package com.backend.interactionservice.post.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;

import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commondataaccess.persistence.post.Post;
import com.backend.commonelasticsearch.operation.bulk.BulkOperationResult;
import com.backend.commonelasticsearch.provision.ElasticsearchIndexProvisioner;
import com.backend.interactionservice.post.repository.PostCountsElasticsearchRepository;
import com.backend.interactionservice.post.repository.PostQueryRepository;
import com.backend.interactionservice.post.service.dto.PostCountSyncResult;
import java.util.List;
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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("PostCountsSyncService 테스트")
@ExtendWith(MockitoExtension.class)
class PostCountsSyncServiceTest {

    private static final int POST_BATCH_SIZE = 2;

    @InjectMocks
    private PostCountsSyncService postCountsSyncService;

    @Mock
    private PostQueryRepository postQueryRepository;

    @Mock
    private PostCountsElasticsearchRepository postCountsElasticsearchRepository;

    @Mock
    private ElasticsearchIndexProvisioner elasticsearchIndexProvisioner;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(postCountsSyncService, "postBatchSize", POST_BATCH_SIZE);
    }

    private Post createPost() {
        return Post.builder()
                   .id(UUID.randomUUID())
                   .postStatus(PostStatus.ACTIVE)
                   .build();
    }

    @DisplayName("syncAll 테스트")
    @Nested
    class SyncAllTest {

        @Test
        void ES_alias가_없으면_sync를_건너뛴다() {
            // given
            Mockito.doReturn(false).when(elasticsearchIndexProvisioner).aliasExists();

            // when
            PostCountSyncResult result = postCountsSyncService.syncAll();

            // then
            Assertions.assertThat(result).isEqualTo(PostCountSyncResult.skipped());
            Mockito.verify(postQueryRepository, Mockito.never()).fetchActivePostsAfterId(any(), anyInt());
            Mockito.verify(postCountsElasticsearchRepository, Mockito.never()).bulkUpsertCounts(anyList());
        }

        @Test
        void 모든_batch를_keyset_pagination으로_순회하며_결과를_합산한다() {
            // given
            Post post1 = createPost();
            Post post2 = createPost();
            Post post3 = createPost();
            List<Post> firstBatch = List.of(post1, post2);
            List<Post> secondBatch = List.of(post3);

            Mockito.doReturn(true).when(elasticsearchIndexProvisioner).aliasExists();
            Mockito.doReturn(firstBatch).when(postQueryRepository).fetchActivePostsAfterId(isNull(), eq(POST_BATCH_SIZE));
            Mockito.doReturn(secondBatch).when(postQueryRepository).fetchActivePostsAfterId(post2.id(), POST_BATCH_SIZE);
            Mockito.doReturn(List.of()).when(postQueryRepository).fetchActivePostsAfterId(post3.id(), POST_BATCH_SIZE);
            Mockito.doReturn(new BulkOperationResult(Set.of(), 2)).when(postCountsElasticsearchRepository).bulkUpsertCounts(firstBatch);
            Mockito.doReturn(new BulkOperationResult(Set.of(post3.id()), 0)).when(postCountsElasticsearchRepository).bulkUpsertCounts(secondBatch);

            // when
            PostCountSyncResult result = postCountsSyncService.syncAll();

            // then
            Assertions.assertThat(result.totalPosts()).isEqualTo(3);
            Assertions.assertThat(result.successCount()).isEqualTo(2);
            Assertions.assertThat(result.failedCount()).isEqualTo(1);
        }

        @Test
        void 동기화할_post가_없으면_0건_결과를_반환한다() {
            // given
            Mockito.doReturn(true).when(elasticsearchIndexProvisioner).aliasExists();
            Mockito.doReturn(List.of()).when(postQueryRepository).fetchActivePostsAfterId(isNull(), eq(POST_BATCH_SIZE));

            // when
            PostCountSyncResult result = postCountsSyncService.syncAll();

            // then
            Assertions.assertThat(result.totalPosts()).isZero();
            Assertions.assertThat(result.successCount()).isZero();
            Assertions.assertThat(result.failedCount()).isZero();
            Mockito.verify(postCountsElasticsearchRepository, Mockito.never()).bulkUpsertCounts(anyList());
        }
    }
}
