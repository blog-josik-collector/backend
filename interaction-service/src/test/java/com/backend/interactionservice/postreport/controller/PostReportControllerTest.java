package com.backend.interactionservice.postreport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.PostReportType;
import com.backend.commondataaccess.persistence.common.enums.ReportStatus;
import com.backend.commondataaccess.security.MockJwtPrincipalResolver;
import com.backend.interactionservice.postreport.controller.dto.PostReportCreateDto;
import com.backend.interactionservice.postreport.controller.dto.PostReportUpdateDto;
import com.backend.interactionservice.postreport.service.PostReportService;
import com.backend.interactionservice.postreport.service.dto.PostReportDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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

@DisplayName("PostReportController 테스트")
@ExtendWith(MockitoExtension.class)
class PostReportControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private PostReportController postReportController;

    @Mock
    private PostReportService postReportService;

    private PostReportDto mockPostReportDto;

    @BeforeEach
    void init() {
        mockPostReportDto = new PostReportDto(UUID.randomUUID(),
                                              "post_title",
                                              MockJwtPrincipalResolver.NICKNAME,
                                              ReportStatus.PENDING,
                                              PostReportType.BROKEN_LINK,
                                              "링크가 깨져 있습니다.",
                                              null,
                                              null);

        mockMvc = MockMvcBuilders.standaloneSetup(postReportController)
                                 .setCustomArgumentResolvers(new MockJwtPrincipalResolver(),
                                                             new PageableHandlerMethodArgumentResolver())
                                 .build();
    }

    @Test
    void 게시글을_신고한다() throws Exception {
        UUID postId = UUID.randomUUID();
        PostReportCreateDto.Request request = new PostReportCreateDto.Request(PostReportType.BROKEN_LINK, "링크가 깨져 있습니다.");

        Mockito.doReturn(mockPostReportDto)
               .when(postReportService).createReport(MockJwtPrincipalResolver.USER_ID, postId, PostReportType.BROKEN_LINK, "링크가 깨져 있습니다.");

        mockMvc.perform(post("/interaction/v1/postings/{postId}/reports", postId)
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(mockPostReportDto.id().toString()))
               .andExpect(jsonPath("$.createdAt").value(mockPostReportDto.createdAt()));
    }

    @Test
    void 게시글_신고_목록을_조회한다() throws Exception {
        OffsetPageResult<PostReportDto> pageResult = new OffsetPageResult<>(1L, 0, 20, List.of(mockPostReportDto));

        Mockito.doReturn(pageResult)
               .when(postReportService).getReports(eq(ReportStatus.PENDING),
                                                   eq(PostReportType.BROKEN_LINK),
                                                   eq(LocalDate.of(2026, 5, 1)),
                                                   eq(LocalDate.of(2026, 5, 31)),
                                                   any(Pageable.class));

        mockMvc.perform(get("/interaction/v1/admin/reports/postings")
                                .param("status", "PENDING")
                                .param("report_type", "BROKEN_LINK")
                                .param("start_date", "2026-05-01")
                                .param("end_date", "2026-05-31")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalCount").value(1))
               .andExpect(jsonPath("$.items[0].id").value(mockPostReportDto.id().toString()))
               .andExpect(jsonPath("$.items[0].title").value(mockPostReportDto.title()))
               .andExpect(jsonPath("$.items[0].status").value(ReportStatus.PENDING.getName()))
               .andExpect(jsonPath("$.items[0].reportType").value(PostReportType.BROKEN_LINK.getName()));
    }

    @Test
    void 게시글_신고_상태를_변경한다() throws Exception {
        PostReportUpdateDto.Request request = new PostReportUpdateDto.Request(ReportStatus.RESOLVED_DELETED);
        PostReportDto changed = new PostReportDto(mockPostReportDto.id(),
                                                  mockPostReportDto.title(),
                                                  mockPostReportDto.nickname(),
                                                  ReportStatus.RESOLVED_DELETED,
                                                  mockPostReportDto.reportType(),
                                                  mockPostReportDto.content(),
                                                  null,
                                                  null);

        Mockito.doReturn(changed)
               .when(postReportService).changeStatus(mockPostReportDto.id(), ReportStatus.RESOLVED_DELETED);

        mockMvc.perform(patch("/interaction/v1/admin/reports/postings/{reportId}", mockPostReportDto.id())
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.id").value(mockPostReportDto.id().toString()))
               .andExpect(jsonPath("$.status").value(ReportStatus.RESOLVED_DELETED.getName()));
    }
}
