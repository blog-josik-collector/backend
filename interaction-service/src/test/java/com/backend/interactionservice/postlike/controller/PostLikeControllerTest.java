package com.backend.interactionservice.postlike.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.security.MockJwtPrincipalResolver;
import com.backend.interactionservice.postlike.service.PostLikeService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("PostLikeController 테스트")
@ExtendWith(MockitoExtension.class)
class PostLikeControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private PostLikeController postLikeController;

    @Mock
    private PostLikeService postLikeService;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(postLikeController)
                                 .setCustomArgumentResolvers(new MockJwtPrincipalResolver())
                                 .build();
    }

    @Test
    void 포스팅에_좋아요를_누른다() throws Exception {
        UUID postId = UUID.randomUUID();

        Mockito.doNothing().when(postLikeService).like(MockJwtPrincipalResolver.USER_ID, postId);

        mockMvc.perform(post("/interaction/v1/postings/{postId}/likes", postId)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted());

        Mockito.verify(postLikeService).like(MockJwtPrincipalResolver.USER_ID, postId);
    }

    @Test
    void 포스팅_좋아요를_취소한다() throws Exception {
        UUID postId = UUID.randomUUID();

        Mockito.doNothing().when(postLikeService).unLike(MockJwtPrincipalResolver.USER_ID, postId);

        mockMvc.perform(delete("/interaction/v1/postings/{postId}/likes", postId)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted());

        Mockito.verify(postLikeService).unLike(MockJwtPrincipalResolver.USER_ID, postId);
    }
}
