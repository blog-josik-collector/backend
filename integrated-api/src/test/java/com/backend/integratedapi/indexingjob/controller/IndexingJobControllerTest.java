package com.backend.integratedapi.indexingjob.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.IndexingJobType;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.persistence.indexingjob.IndexingJob;
import com.backend.commondataaccess.security.MockJwtPrincipalResolver;
import com.backend.integratedapi.indexingjob.service.IndexingJobService;
import com.backend.integratedapi.indexingjob.service.dto.IndexingJobDto;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("IndexingJobController 테스트")
@ExtendWith(MockitoExtension.class)
class IndexingJobControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private IndexingJobController indexingJobController;

    @Mock
    private IndexingJobService indexingJobService;

    private IndexingJobDto mockIndexingJobDto;
    private IndexingJob mockIndexingJob;

    @BeforeEach
    void init() {
        UUID jobId = UUID.randomUUID();
        UUID sourceId = UUID.randomUUID();
        UUID triggeredBy = UUID.randomUUID();

        mockIndexingJobDto = IndexingJobDto.builder()
                                           .id(jobId)
                                           .indexingJobType(IndexingJobType.MANUAL)
                                           .jobStatus(JobStatus.PENDING)
                                           .totalCount(10)
                                           .indexedCount(0)
                                           .triggeredBy(triggeredBy)
                                           .targetSourceId(sourceId)
                                           .targetPostId(null)
                                           .build();

        mockIndexingJob = IndexingJob.builder()
                                     .id(jobId)
                                     .indexingJobType(IndexingJobType.MANUAL)
                                     .jobStatus(JobStatus.PENDING)
                                     .build();

        mockMvc = MockMvcBuilders.standaloneSetup(indexingJobController)
                                 .setCustomArgumentResolvers(new MockJwtPrincipalResolver())
                                 .build();
    }

    @Test
    void source_전체_재색인_작업을_시작한다() throws Exception {
        UUID sourceId = UUID.randomUUID();

        Mockito.doReturn(mockIndexingJob).when(indexingJobService).triggerReindexByCollectSource(Mockito.eq(sourceId), any(UUID.class));

        mockMvc.perform(post("/index/v1/sources/{source-id}/_reindex", sourceId)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted())
               .andExpect(jsonPath("$.jobId").value(mockIndexingJob.id().toString()))
               .andExpect(jsonPath("$.jobStatus").value(JobStatus.PENDING.getName()));

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        Mockito.verify(indexingJobService).triggerReindexByCollectSource(Mockito.eq(sourceId), userIdCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(userIdCaptor.getValue()).isNotNull();
    }

    @Test
    void post_단건_재색인_작업을_시작한다() throws Exception {
        UUID postId = UUID.randomUUID();

        Mockito.doReturn(mockIndexingJob).when(indexingJobService).triggerReindexByCollectSourcePost(Mockito.eq(postId), any(UUID.class));

        mockMvc.perform(post("/index/v1/posts/{post-id}/_reindex", postId)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted())
               .andExpect(jsonPath("$.jobId").value(mockIndexingJob.id().toString()))
               .andExpect(jsonPath("$.jobStatus").value(JobStatus.PENDING.getName()));

        ArgumentCaptor<UUID> userIdCaptor = ArgumentCaptor.forClass(UUID.class);
        Mockito.verify(indexingJobService).triggerReindexByCollectSourcePost(Mockito.eq(postId), userIdCaptor.capture());
        org.assertj.core.api.Assertions.assertThat(userIdCaptor.getValue()).isNotNull();
    }

    @Test
    void 색인_작업_상태_목록을_조회한다() throws Exception {
        OffsetPageResult<IndexingJobDto> pageResult =
                new OffsetPageResult<>(1L, 0, 10, List.of(mockIndexingJobDto));

        Mockito.doReturn(pageResult).when(indexingJobService).getIndexingJobs(anyInt(), anyInt());

        mockMvc.perform(get("/index/v1/jobs")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalCount").value(1))
               .andExpect(jsonPath("$.items[0].jobId").value(mockIndexingJobDto.id().toString()))
               .andExpect(jsonPath("$.items[0].jobStatus").value(mockIndexingJobDto.jobStatus().getName()))
               .andExpect(jsonPath("$.items[0].indexingJobType").value(mockIndexingJobDto.indexingJobType().getName()))
               .andExpect(jsonPath("$.items[0].totalCount").value(mockIndexingJobDto.totalCount()))
               .andExpect(jsonPath("$.items[0].indexedCount").value(mockIndexingJobDto.indexedCount()))
               .andExpect(jsonPath("$.items[0].triggeredBy").value(mockIndexingJobDto.triggeredBy().toString()))
               .andExpect(jsonPath("$.items[0].targetSourceId").value(mockIndexingJobDto.targetSourceId().toString()));
    }

    @Test
    void 색인_작업_상태를_조회한다() throws Exception {
        Mockito.doReturn(mockIndexingJobDto).when(indexingJobService).getIndexingJobDto(any(UUID.class));

        mockMvc.perform(get("/index/v1/jobs/{id}", mockIndexingJobDto.id())
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.jobId").value(mockIndexingJobDto.id().toString()))
               .andExpect(jsonPath("$.jobStatus").value(mockIndexingJobDto.jobStatus().getName()))
               .andExpect(jsonPath("$.indexingJobType").value(mockIndexingJobDto.indexingJobType().getName()))
               .andExpect(jsonPath("$.totalCount").value(mockIndexingJobDto.totalCount()))
               .andExpect(jsonPath("$.indexedCount").value(mockIndexingJobDto.indexedCount()))
               .andExpect(jsonPath("$.triggeredBy").value(mockIndexingJobDto.triggeredBy().toString()))
               .andExpect(jsonPath("$.targetSourceId").value(mockIndexingJobDto.targetSourceId().toString()));
    }
}
