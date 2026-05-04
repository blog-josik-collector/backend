package com.backend.integratedapi.collectsourcepost.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.persistence.common.enums.IndexingStatus;
import com.backend.integratedapi.collectsourcepost.service.CollectSourcePostService;
import com.backend.integratedapi.collectsourcepost.service.dto.CollectSourcePostDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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

@DisplayName("CollectSourcePostController 테스트")
@ExtendWith(MockitoExtension.class)
class CollectSourcePostControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private CollectSourcePostController collectSourcePostController;

    @Mock
    private CollectSourcePostService collectSourcePostService;

    private CollectSourcePostDto mockCollectSourcePostDto;

    @BeforeEach
    void init() {
        mockCollectSourcePostDto = CollectSourcePostDto.builder()
                                                       .id(UUID.randomUUID())
                                                       .collectSourceId(UUID.randomUUID())
                                                       .title("test_title")
                                                       .url("https://test.com/blog/1/post/1")
                                                       .publishedAt(LocalDate.of(2025, 1, 1))
                                                       .thumbnailUrl("https://test.com/thumb.png")
                                                       .summary("test_summary")
                                                       .indexingStatus(IndexingStatus.INDEX_PENDING)
                                                       .indexingErrorCount(0)
                                                       .lastCollectingJobId(UUID.randomUUID())
                                                       .build();

        mockMvc = MockMvcBuilders.standaloneSetup(collectSourcePostController).build();
    }

    @Test
    void 수집_결과_한건을_조회한다() throws Exception {
        Mockito.doReturn(mockCollectSourcePostDto)
               .when(collectSourcePostService).getCollectSourcePostDto(any(UUID.class));

        mockMvc.perform(get("/collect/v1/postings/{id}", mockCollectSourcePostDto.id())
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.postingId").value(mockCollectSourcePostDto.id().toString()))
               .andExpect(jsonPath("$.collectSourceId").value(mockCollectSourcePostDto.collectSourceId().toString()))
               .andExpect(jsonPath("$.title").value(mockCollectSourcePostDto.title()))
               .andExpect(jsonPath("$.url").value(mockCollectSourcePostDto.url()))
               .andExpect(jsonPath("$.thumbnailUrl").value(mockCollectSourcePostDto.thumbnailUrl()))
               .andExpect(jsonPath("$.summary").value(mockCollectSourcePostDto.summary()))
               .andExpect(jsonPath("$.indexingStatus").value(mockCollectSourcePostDto.indexingStatus().getName()))
               .andExpect(jsonPath("$.lastCollectingJobId").value(mockCollectSourcePostDto.lastCollectingJobId().toString()));
    }
}
