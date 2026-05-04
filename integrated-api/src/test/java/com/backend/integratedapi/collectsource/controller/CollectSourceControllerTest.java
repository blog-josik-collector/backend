package com.backend.integratedapi.collectsource.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.commondataaccess.persistence.common.enums.ScheduleType;
import com.backend.integratedapi.collectsource.controller.dto.CollectSourceCreateDto;
import com.backend.integratedapi.collectsource.controller.dto.CollectSourceUpdateDto;
import com.backend.integratedapi.collectsource.service.CollectSourceService;
import com.backend.integratedapi.collectsource.service.dto.CollectSourceDto;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@DisplayName("CollectSourceController 테스트")
@ExtendWith(MockitoExtension.class)
class CollectSourceControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private CollectSourceController collectSourceController;

    @Mock
    private CollectSourceService collectSourceService;

    private CollectSourceDto mockCollectSourceDto;

    @BeforeEach
    void init() {
        mockCollectSourceDto = CollectSourceDto.builder()
                                               .id(UUID.randomUUID())
                                               .providerId(UUID.randomUUID())
                                               .url("https://test.com/blog/1")
                                               .scheduleType(ScheduleType.CRON)
                                               .cronExpression("0 0 * * * *")
                                               .isUsed(true)
                                               .build();

        mockMvc = MockMvcBuilders.standaloneSetup(collectSourceController).build();
    }

    @Test
    void 수집_소스를_등록한다() throws Exception {
        CollectSourceCreateDto.Request request = new CollectSourceCreateDto.Request(mockCollectSourceDto.providerId(),
                                                                                    mockCollectSourceDto.url(),
                                                                                    mockCollectSourceDto.scheduleType(),
                                                                                    mockCollectSourceDto.cronExpression());

        Mockito.doReturn(mockCollectSourceDto).when(collectSourceService).create(any(CollectSourceDto.class));

        mockMvc.perform(post("/collect/v1/sources")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.sourceId").value(mockCollectSourceDto.id().toString()))
               .andExpect(jsonPath("$.createdAt").value(mockCollectSourceDto.createdAt()));
    }

    @Test
    void 수집_소스_목록을_조회한다() throws Exception {
        OffsetPageResult<CollectSourceDto> pageResult = new OffsetPageResult<>(1L, 0, 10, List.of(mockCollectSourceDto));

        Mockito.doReturn(pageResult).when(collectSourceService).getCollectSources(0, 10);

        mockMvc.perform(get("/collect/v1/sources")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalCount").value(1))
               .andExpect(jsonPath("$.items[0].sourceId").value(mockCollectSourceDto.id().toString()))
               .andExpect(jsonPath("$.items[0].providerId").value(mockCollectSourceDto.providerId().toString()))
               .andExpect(jsonPath("$.items[0].url").value(mockCollectSourceDto.url()))
               .andExpect(jsonPath("$.items[0].scheduleType").value(mockCollectSourceDto.scheduleType().getName()))
               .andExpect(jsonPath("$.items[0].cronExpression").value(mockCollectSourceDto.cronExpression()));
    }

    @Test
    void 수집_소스_한건을_조회한다() throws Exception {
        Mockito.doReturn(mockCollectSourceDto).when(collectSourceService).getCollectSourceDto(any(UUID.class));

        mockMvc.perform(get("/collect/v1/sources/{id}", mockCollectSourceDto.id())
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.sourceId").value(mockCollectSourceDto.id().toString()))
               .andExpect(jsonPath("$.providerId").value(mockCollectSourceDto.providerId().toString()))
               .andExpect(jsonPath("$.url").value(mockCollectSourceDto.url()))
               .andExpect(jsonPath("$.scheduleType").value(mockCollectSourceDto.scheduleType().getName()))
               .andExpect(jsonPath("$.cronExpression").value(mockCollectSourceDto.cronExpression()))
               .andExpect(jsonPath("$.isUsed").value(mockCollectSourceDto.isUsed()));
    }

    @Test
    void 수집_소스를_수정한다() throws Exception {
        CollectSourceUpdateDto.Request request = new CollectSourceUpdateDto.Request("https://updated.com",
                                                                                    ScheduleType.CRON,
                                                                                    "0 30 * * * *",
                                                                                    true);

        Mockito.doNothing().when(collectSourceService).update(any(CollectSourceDto.class));
        Mockito.doReturn(mockCollectSourceDto).when(collectSourceService).getCollectSourceDto(any(UUID.class));

        mockMvc.perform(patch("/collect/v1/sources/{id}", mockCollectSourceDto.id())
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.sourceId").value(mockCollectSourceDto.id().toString()))
               .andExpect(jsonPath("$.updatedAt").value(mockCollectSourceDto.updatedAt()));
    }

    @Test
    void 수집_소스를_삭제한다() throws Exception {
        Mockito.doNothing().when(collectSourceService).delete(any(UUID.class));

        mockMvc.perform(delete("/collect/v1/sources/{id}", mockCollectSourceDto.id())
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted());
    }
}
