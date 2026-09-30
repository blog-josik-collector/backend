package com.backend.interactionservice.commentreport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.CommentReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.security.MockJwtPrincipalResolver;
import com.backend.interactionservice.commentreport.controller.dto.CommentReportCreateDto;
import com.backend.interactionservice.commentreport.controller.dto.CommentReportUpdateDto;
import com.backend.interactionservice.commentreport.service.CommentReportService;
import com.backend.interactionservice.commentreport.service.dto.CommentReportDto;
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

@DisplayName("CommentReportController 테스트")
@ExtendWith(MockitoExtension.class)
class CommentReportControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private CommentReportController commentReportController;

    @Mock
    private CommentReportService commentReportService;

    private CommentReportDto mockCommentReportDto;

    @BeforeEach
    void init() {
        mockCommentReportDto = new CommentReportDto(UUID.randomUUID(),
                                                    "test_comment",
                                                    MockJwtPrincipalResolver.NICKNAME,
                                                    ReportStatus.PENDING,
                                                    CommentReportType.ADULT,
                                                    "광고성 댓글입니다.",
                                                    null,
                                                    null);

        mockMvc = MockMvcBuilders.standaloneSetup(commentReportController)
                                 .setCustomArgumentResolvers(new MockJwtPrincipalResolver(),
                                                             new PageableHandlerMethodArgumentResolver())
                                 .build();
    }

    @Test
    void 댓글을_신고한다() throws Exception {
        UUID commentId = UUID.randomUUID();
        CommentReportCreateDto.Request request = new CommentReportCreateDto.Request(CommentReportType.ADULT, "광고성 댓글입니다.");

        Mockito.doReturn(mockCommentReportDto)
               .when(commentReportService).createReport(MockJwtPrincipalResolver.USER_ID, commentId, CommentReportType.ADULT, "광고성 댓글입니다.");

        mockMvc.perform(post("/interaction/v1/comments/{commentId}/reports", commentId)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(mockCommentReportDto.id().toString()))
               .andExpect(jsonPath("$.createdAt").value(mockCommentReportDto.createdAt()));
    }

    @Test
    void 댓글_신고_목록을_조회한다() throws Exception {
        OffsetPageResult<CommentReportDto> pageResult = new OffsetPageResult<>(1L, 0, 20, List.of(mockCommentReportDto));

        Mockito.doReturn(pageResult)
               .when(commentReportService).getReports(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));

        mockMvc.perform(get("/interaction/v1/admin/reports/comments")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalCount").value(1))
               .andExpect(jsonPath("$.items[0].id").value(mockCommentReportDto.id().toString()))
               .andExpect(jsonPath("$.items[0].commentContent").value(mockCommentReportDto.commentContent()))
               .andExpect(jsonPath("$.items[0].reportType").value(CommentReportType.ADULT.getName()));
    }

    @Test
    void 댓글_신고_상태를_변경한다() throws Exception {
        CommentReportUpdateDto.Request request = new CommentReportUpdateDto.Request(ReportStatus.REJECTED_KEEP);
        CommentReportDto changed = new CommentReportDto(mockCommentReportDto.id(),
                                                        mockCommentReportDto.commentContent(),
                                                        mockCommentReportDto.nickname(),
                                                        ReportStatus.REJECTED_KEEP,
                                                        mockCommentReportDto.reportType(),
                                                        mockCommentReportDto.content(),
                                                        null,
                                                        null);

        Mockito.doReturn(changed)
               .when(commentReportService).changeStatus(eq(mockCommentReportDto.id()), eq(ReportStatus.REJECTED_KEEP));

        mockMvc.perform(patch("/interaction/v1/admin/reports/comments/{reportId}", mockCommentReportDto.id())
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(mockCommentReportDto.id().toString()))
               .andExpect(jsonPath("$.status").value(ReportStatus.REJECTED_KEEP.getName()));
    }
}
