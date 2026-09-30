package com.backend.interactionservice.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commondataaccess.security.MockJwtPrincipalResolver;
import com.backend.interactionservice.post.repository.query.SearchCondition;
import com.backend.interactionservice.post.service.PostService;
import com.backend.interactionservice.post.service.PostViewCountService;
import com.backend.interactionservice.post.service.dto.PostListItem;
import java.util.List;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("PostController 테스트")
@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private PostController postController;

    @Mock
    private PostService postService;

    @Mock
    private PostViewCountService postViewCountService;

    private PostListItem mockPostListItem;

    @BeforeEach
    void init() {
        mockPostListItem = new PostListItem(UUID.randomUUID(),
                                            "Spring Boot에서 Clean Architecture 적용하기",
                                            "https://techblog.com/clean-architecture",
                                            null,
                                            "summary",
                                            "Toss",
                                            PostStatus.ACTIVE,
                                            null,
                                            null,
                                            null,
                                            15,
                                            120,
                                            3,
                                            0,
                                            true,
                                            false);

        mockMvc = MockMvcBuilders.standaloneSetup(postController)
                                 .setCustomArgumentResolvers(new MockJwtPrincipalResolver(),
                                                             new PageableHandlerMethodArgumentResolver())
                                 .build();
    }

    @Test
    void 포스팅_목록을_검색한다() throws Exception {
        OffsetPageResult<PostListItem> pageResult = new OffsetPageResult<>(1L, 0, 20, List.of(mockPostListItem));

        Mockito.doReturn(pageResult)
               .when(postService).searchPosts(any(SearchCondition.class), eq(MockJwtPrincipalResolver.USER_ID), any(Pageable.class));

        mockMvc.perform(get("/interaction/v1/postings")
                                .param("title", "spring")
                                .param("provider", "Toss")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalCount").value(1))
               .andExpect(jsonPath("$.items[0].id").value(mockPostListItem.id().toString()))
               .andExpect(jsonPath("$.items[0].title").value(mockPostListItem.title()))
               .andExpect(jsonPath("$.items[0].provider").value(mockPostListItem.provider()))
               .andExpect(jsonPath("$.items[0].likesOfMe").value(true))
               .andExpect(jsonPath("$.items[0].bookmarksOfMe").value(false));

        ArgumentCaptor<SearchCondition> conditionCaptor = ArgumentCaptor.forClass(SearchCondition.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        Mockito.verify(postService).searchPosts(conditionCaptor.capture(), eq(MockJwtPrincipalResolver.USER_ID), pageableCaptor.capture());
        Assertions.assertThat(conditionCaptor.getValue().getTitle()).isEqualTo("spring");
        Assertions.assertThat(conditionCaptor.getValue().getProvider()).isEqualTo("Toss");
        Assertions.assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
    }

    @Test
    void 포스팅_한건을_조회하고_조회수를_기록한다() throws Exception {
        Mockito.doReturn(mockPostListItem)
               .when(postService).searchPost(mockPostListItem.id(), MockJwtPrincipalResolver.USER_ID);

        mockMvc.perform(get("/interaction/v1/postings/{postId}", mockPostListItem.id())
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(mockPostListItem.id().toString()))
               .andExpect(jsonPath("$.status").value(PostStatus.ACTIVE.getName()))
               .andExpect(jsonPath("$.likeCount").value(mockPostListItem.likeCount()))
               .andExpect(jsonPath("$.viewCount").value(mockPostListItem.viewCount()))
               .andExpect(jsonPath("$.likesOfMe").value(true));

        Mockito.verify(postViewCountService).recordView(mockPostListItem.id());
    }
}
