package com.backend.interactionservice.postcomment.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.PostCommentStatus;
import com.backend.commondataaccess.security.MockJwtPrincipalResolver;
import com.backend.interactionservice.postcomment.constant.PostCommentConstant;
import com.backend.interactionservice.postcomment.controller.dto.PostCommentCreateDto;
import com.backend.interactionservice.postcomment.controller.dto.PostCommentUpdateDto;
import com.backend.interactionservice.postcomment.service.PostCommentService;
import com.backend.interactionservice.postcomment.service.dto.PostCommentDto;
import com.fasterxml.jackson.databind.ObjectMapper;
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

@DisplayName("PostCommentController 테스트")
@ExtendWith(MockitoExtension.class)
class PostCommentControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private PostCommentController postCommentController;

    @Mock
    private PostCommentService postCommentService;

    private PostCommentDto mockCommentDto;
    private PostCommentDto mockReplyDto;

    @BeforeEach
    void init() {
        UUID postId = UUID.randomUUID();
        UUID commentId = UUID.randomUUID();

        mockCommentDto = new PostCommentDto(commentId,
                                            postId,
                                            null,
                                            MockJwtPrincipalResolver.USER_ID,
                                            MockJwtPrincipalResolver.NICKNAME,
                                            "test_comment",
                                            PostCommentStatus.ACTIVE,
                                            false,
                                            true,
                                            null,
                                            null);

        mockReplyDto = new PostCommentDto(UUID.randomUUID(),
                                          postId,
                                          commentId,
                                          MockJwtPrincipalResolver.USER_ID,
                                          MockJwtPrincipalResolver.NICKNAME,
                                          "test_reply",
                                          PostCommentStatus.ACTIVE,
                                          true,
                                          false,
                                          null,
                                          null);

        mockMvc = MockMvcBuilders.standaloneSetup(postCommentController)
                                 .setCustomArgumentResolvers(new MockJwtPrincipalResolver(),
                                                             new PageableHandlerMethodArgumentResolver())
                                 .build();
    }

    @Test
    void 댓글을_작성한다() throws Exception {
        PostCommentCreateDto.Request request = new PostCommentCreateDto.Request("test_comment");

        Mockito.doReturn(mockCommentDto)
               .when(postCommentService).createComment(MockJwtPrincipalResolver.USER_ID, mockCommentDto.postId(), "test_comment");

        mockMvc.perform(post("/interaction/v1/postings/{postId}/comments", mockCommentDto.postId())
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(mockCommentDto.id().toString()))
               .andExpect(jsonPath("$.createdAt").value(mockCommentDto.createdAt()));
    }

    @Test
    void 댓글_목록을_조회한다() throws Exception {
        PostCommentDto deletedDto = new PostCommentDto(UUID.randomUUID(),
                                                       mockCommentDto.postId(),
                                                       null,
                                                       MockJwtPrincipalResolver.USER_ID,
                                                       MockJwtPrincipalResolver.NICKNAME,
                                                       "원래 내용",
                                                       PostCommentStatus.DELETED,
                                                       false,
                                                       false,
                                                       null,
                                                       null);
        OffsetPageResult<PostCommentDto> pageResult = new OffsetPageResult<>(2L, 0, 20, List.of(mockCommentDto, deletedDto));

        Mockito.doReturn(pageResult)
               .when(postCommentService).getComments(eq(mockCommentDto.postId()), any(Pageable.class));

        mockMvc.perform(get("/interaction/v1/postings/{postId}/comments", mockCommentDto.postId())
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalCount").value(2))
               .andExpect(jsonPath("$.items[0].id").value(mockCommentDto.id().toString()))
               .andExpect(jsonPath("$.items[0].nickname").value(MockJwtPrincipalResolver.NICKNAME))
               .andExpect(jsonPath("$.items[0].content").value(mockCommentDto.content()))
               .andExpect(jsonPath("$.items[0].hasParentComment").value(false))
               .andExpect(jsonPath("$.items[0].hasChildComment").value(true))
               .andExpect(jsonPath("$.items[0].status").value(PostCommentStatus.ACTIVE.getName()))
               // 삭제된 댓글은 본문이 치환된다.
               .andExpect(jsonPath("$.items[1].content").value(PostCommentConstant.DELETED_COMMENT_CONTENT))
               .andExpect(jsonPath("$.items[1].status").value(PostCommentStatus.DELETED.getName()));
    }

    @Test
    void 댓글을_수정한다() throws Exception {
        PostCommentUpdateDto.Request request = new PostCommentUpdateDto.Request("updated");

        Mockito.doReturn(mockCommentDto)
               .when(postCommentService).updateComment(MockJwtPrincipalResolver.USER_ID, mockCommentDto.id(), "updated");

        mockMvc.perform(patch("/interaction/v1/comments/{commentId}", mockCommentDto.id())
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(mockCommentDto.id().toString()))
               .andExpect(jsonPath("$.updatedAt").value(mockCommentDto.updatedAt()));
    }

    @Test
    void 댓글을_삭제한다() throws Exception {
        Mockito.doNothing().when(postCommentService).deleteComment(MockJwtPrincipalResolver.USER_ID, mockCommentDto.id());

        mockMvc.perform(delete("/interaction/v1/comments/{commentId}", mockCommentDto.id())
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted());

        Mockito.verify(postCommentService).deleteComment(MockJwtPrincipalResolver.USER_ID, mockCommentDto.id());
    }

    @Test
    void 대댓글을_작성한다() throws Exception {
        PostCommentCreateDto.Request request = new PostCommentCreateDto.Request("test_reply");

        Mockito.doReturn(mockReplyDto)
               .when(postCommentService).createReply(MockJwtPrincipalResolver.USER_ID, mockCommentDto.id(), "test_reply");

        mockMvc.perform(post("/interaction/v1/comments/{commentId}/replies", mockCommentDto.id())
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(mockReplyDto.id().toString()))
               .andExpect(jsonPath("$.parentId").value(mockCommentDto.id().toString()));
    }

    @Test
    void 대댓글_목록을_조회한다() throws Exception {
        OffsetPageResult<PostCommentDto> pageResult = new OffsetPageResult<>(1L, 0, 20, List.of(mockReplyDto));

        Mockito.doReturn(pageResult)
               .when(postCommentService).getReplies(eq(mockCommentDto.id()), any(Pageable.class));

        mockMvc.perform(get("/interaction/v1/comments/{commentId}/replies", mockCommentDto.id())
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalCount").value(1))
               .andExpect(jsonPath("$.items[0].id").value(mockReplyDto.id().toString()))
               .andExpect(jsonPath("$.items[0].hasParentComment").value(true));
    }

    @Test
    void 대댓글을_수정한다() throws Exception {
        PostCommentUpdateDto.Request request = new PostCommentUpdateDto.Request("updated_reply");

        Mockito.doReturn(mockReplyDto)
               .when(postCommentService).updateReply(MockJwtPrincipalResolver.USER_ID, mockReplyDto.id(), "updated_reply");

        mockMvc.perform(patch("/interaction/v1/replies/{replyId}", mockReplyDto.id())
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(mockReplyDto.id().toString()));
    }

    @Test
    void 대댓글을_삭제한다() throws Exception {
        Mockito.doNothing().when(postCommentService).deleteReply(MockJwtPrincipalResolver.USER_ID, mockReplyDto.id());

        mockMvc.perform(delete("/interaction/v1/replies/{replyId}", mockReplyDto.id())
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted());

        Mockito.verify(postCommentService).deleteReply(MockJwtPrincipalResolver.USER_ID, mockReplyDto.id());
    }

    @Test
    void 내가_작성한_댓글_목록을_조회한다() throws Exception {
        OffsetPageResult<PostCommentDto> pageResult = new OffsetPageResult<>(2L, 0, 20, List.of(mockReplyDto, mockCommentDto));

        Mockito.doReturn(pageResult)
               .when(postCommentService).getMyComments(eq(MockJwtPrincipalResolver.USER_ID), any(Pageable.class));

        mockMvc.perform(get("/interaction/v1/me/comments")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalCount").value(2))
               .andExpect(jsonPath("$.items[0].id").value(mockReplyDto.id().toString()))
               .andExpect(jsonPath("$.items[1].id").value(mockCommentDto.id().toString()));
    }
}
