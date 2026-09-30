package com.backend.interactionservice.post.service;

import static org.mockito.ArgumentMatchers.any;

import com.backend.interactionservice.post.repository.PostViewCountRedisRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PostViewCountService 테스트")
@ExtendWith(MockitoExtension.class)
class PostViewCountServiceTest {

    @InjectMocks
    private PostViewCountService postViewCountService;

    @Mock
    private PostViewCountRedisRepository postViewCountRedisRepository;

    @DisplayName("recordView 테스트")
    @Nested
    class RecordViewTest {

        @Test
        void postId를_입력하면_Redis_카운터를_증가시킨다() {
            // given
            UUID postId = UUID.randomUUID();

            // when
            postViewCountService.recordView(postId);

            // then
            Mockito.verify(postViewCountRedisRepository).increment(postId);
        }

        @Test
        void postId가_null이면_아무것도_하지_않는다() {
            // when
            postViewCountService.recordView(null);

            // then
            Mockito.verify(postViewCountRedisRepository, Mockito.never()).increment(any());
        }
    }
}
