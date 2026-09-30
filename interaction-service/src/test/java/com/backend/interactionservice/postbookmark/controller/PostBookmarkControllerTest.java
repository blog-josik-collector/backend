package com.backend.interactionservice.postbookmark.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.PostStatus;
import com.backend.commondataaccess.security.MockJwtPrincipalResolver;
import com.backend.interactionservice.post.service.dto.PostDocument;
import com.backend.interactionservice.postbookmark.service.PostBookmarkService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("PostBookmarkController 테스트")
@ExtendWith(MockitoExtension.class)
class PostBookmarkControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private PostBookmarkController postBookmarkController;

    @Mock
    private PostBookmarkService postBookmarkService;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(postBookmarkController)
                                 .setCustomArgumentResolvers(new MockJwtPrincipalResolver(),
                                                             new PageableHandlerMethodArgumentResolver())
                                 .build();
    }

    @Test
    void 포스팅을_즐겨찾기한다() throws Exception {
        UUID postId = UUID.randomUUID();

        Mockito.doNothing().when(postBookmarkService).bookmark(MockJwtPrincipalResolver.USER_ID, postId);

        mockMvc.perform(post("/interaction/v1/postings/{postId}/bookmarks", postId)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted());

        Mockito.verify(postBookmarkService).bookmark(MockJwtPrincipalResolver.USER_ID, postId);
    }

    @Test
    void 포스팅_즐겨찾기를_취소한다() throws Exception {
        UUID postId = UUID.randomUUID();

        Mockito.doNothing().when(postBookmarkService).unBookmark(MockJwtPrincipalResolver.USER_ID, postId);

        mockMvc.perform(delete("/interaction/v1/postings/{postId}/bookmarks", postId)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted());

        Mockito.verify(postBookmarkService).unBookmark(MockJwtPrincipalResolver.USER_ID, postId);
    }

    @Test
    void 내_즐겨찾기_목록을_조회한다() throws Exception {
        PostDocument document = new PostDocument(UUID.randomUUID(),
                                                 "title",
                                                 "https://test.com/1",
                                                 null,
                                                 "summary",
                                                 "Toss",
                                                 PostStatus.ACTIVE,
                                                 null,
                                                 null,
                                                 null,
                                                 1,
                                                 2,
                                                 3,
                                                 0);
        OffsetPageResult<PostDocument> pageResult = new OffsetPageResult<>(1L, 0, 20, List.of(document));

        Mockito.doReturn(pageResult)
               .when(postBookmarkService).getMyBookmarks(eq(MockJwtPrincipalResolver.USER_ID), any(Pageable.class));

        mockMvc.perform(get("/interaction/v1/me/bookmarks")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalCount").value(1))
               .andExpect(jsonPath("$.items[0].id").value(document.id().toString()))
               .andExpect(jsonPath("$.items[0].title").value(document.title()));
    }
}
