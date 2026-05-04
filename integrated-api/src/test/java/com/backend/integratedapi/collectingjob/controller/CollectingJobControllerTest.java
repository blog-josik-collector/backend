package com.backend.integratedapi.collectingjob.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.JobStatus;
import com.backend.commondataaccess.security.MockJwtPrincipalResolver;
import com.backend.integratedapi.collectingjob.service.CollectingJobService;
import com.backend.integratedapi.collectingjob.service.dto.CollectingJobDto;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("CollectingJobController 테스트")
@ExtendWith(MockitoExtension.class)
class CollectingJobControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private CollectingJobController collectingJobController;

    @Mock
    private CollectingJobService collectingJobService;

    private CollectingJobDto mockCollectingJobDto;

    @BeforeEach
    void init() {
        mockCollectingJobDto = CollectingJobDto.builder()
                                               .id(UUID.randomUUID())
                                               .sourceId(UUID.randomUUID())
                                               .jobStatus(JobStatus.PENDING)
                                               .fromPage(1)
                                               .toPage(5)
                                               .build();

        // @Value 필드는 standaloneSetup에서는 주입되지 않으므로 직접 세팅
        ReflectionTestUtils.setField(collectingJobController, "defaultFromPage", "1");
        ReflectionTestUtils.setField(collectingJobController, "defaultToPage", "10");

        mockMvc = MockMvcBuilders.standaloneSetup(collectingJobController)
                                 .setCustomArgumentResolvers(new MockJwtPrincipalResolver())
                                 .build();
    }

    @Test
    void 수집_작업을_실행한다() throws Exception {
        UUID sourceId = mockCollectingJobDto.sourceId();

        Mockito.doReturn(mockCollectingJobDto).when(collectingJobService).start(any(CollectingJobDto.class));

        mockMvc.perform(post("/collect/v1/sources/{source-id}/_start", sourceId)
                                .param("from_page", "2")
                                .param("to_page", "7")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.jobId").value(mockCollectingJobDto.id().toString()))
               .andExpect(jsonPath("$.jobStatus").value(mockCollectingJobDto.jobStatus().getName()));

        ArgumentCaptor<CollectingJobDto> captor = ArgumentCaptor.forClass(CollectingJobDto.class);
        Mockito.verify(collectingJobService).start(captor.capture());
        CollectingJobDto captured = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(captured.sourceId()).isEqualTo(sourceId);
        org.assertj.core.api.Assertions.assertThat(captured.fromPage()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(captured.toPage()).isEqualTo(7);
        org.assertj.core.api.Assertions.assertThat(captured.userId()).isNotNull();
    }

    @Test
    void from_page와_to_page를_생략하면_기본값으로_수집_작업을_시작한다() throws Exception {
        UUID sourceId = mockCollectingJobDto.sourceId();

        Mockito.doReturn(mockCollectingJobDto).when(collectingJobService).start(any(CollectingJobDto.class));

        mockMvc.perform(post("/collect/v1/sources/{source-id}/_start", sourceId)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.jobId").value(mockCollectingJobDto.id().toString()));

        ArgumentCaptor<CollectingJobDto> captor = ArgumentCaptor.forClass(CollectingJobDto.class);
        Mockito.verify(collectingJobService).start(captor.capture());
        CollectingJobDto captured = captor.getValue();
        org.assertj.core.api.Assertions.assertThat(captured.fromPage()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(captured.toPage()).isEqualTo(10);
    }

    @Test
    void 수집_작업을_종료한다() throws Exception {
        UUID sourceId = UUID.randomUUID();

        Mockito.doNothing().when(collectingJobService).stop(any(UUID.class));

        mockMvc.perform(post("/collect/v1/sources/{source-id}/_stop", sourceId)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted());
    }

    @Test
    void 수집_작업_상태_목록을_조회한다() throws Exception {
        OffsetPageResult<CollectingJobDto> pageResult =
                new OffsetPageResult<>(1L, 0, 10, List.of(mockCollectingJobDto));

        Mockito.doReturn(pageResult).when(collectingJobService).getCollectingJobs(anyInt(), anyInt());

        mockMvc.perform(get("/collect/v1/jobs")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalCount").value(1))
               .andExpect(jsonPath("$.items[0].jobId").value(mockCollectingJobDto.id().toString()))
               .andExpect(jsonPath("$.items[0].jobStatus").value(mockCollectingJobDto.jobStatus().getName()))
               .andExpect(jsonPath("$.items[0].fromPage").value(mockCollectingJobDto.fromPage()))
               .andExpect(jsonPath("$.items[0].toPage").value(mockCollectingJobDto.toPage()));
    }

    @Test
    void 수집_작업_상태를_조회한다() throws Exception {
        Mockito.doReturn(mockCollectingJobDto).when(collectingJobService).getCollectingJobDto(any(UUID.class));

        mockMvc.perform(get("/collect/v1/jobs/{id}", mockCollectingJobDto.id())
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.jobId").value(mockCollectingJobDto.id().toString()))
               .andExpect(jsonPath("$.jobStatus").value(mockCollectingJobDto.jobStatus().getName()))
               .andExpect(jsonPath("$.fromPage").value(mockCollectingJobDto.fromPage()))
               .andExpect(jsonPath("$.toPage").value(mockCollectingJobDto.toPage()));
    }
}
