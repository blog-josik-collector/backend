package com.backend.integratedapi.provider.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.commondataaccess.dto.OffsetPageResult;
import com.backend.integratedapi.provider.controller.dto.PostProviderCreateDto;
import com.backend.integratedapi.provider.controller.dto.PostProviderUpdateDto;
import com.backend.integratedapi.provider.service.PostProviderService;
import com.backend.integratedapi.provider.service.dto.PostProviderDto;
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

@DisplayName("PostProviderController 테스트")
@ExtendWith(MockitoExtension.class)
class PostProviderControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @InjectMocks
    private PostProviderController postProviderController;

    @Mock
    private PostProviderService postProviderService;

    private PostProviderDto mockPostProviderDto;

    @BeforeEach
    void init() {
        mockPostProviderDto = PostProviderDto.builder()
                                             .id(UUID.randomUUID())
                                             .name("test_provider")
                                             .baseUrl("https://test.com")
                                             .description("test_description")
                                             .isUsed(true)
                                             .build();

        mockMvc = MockMvcBuilders.standaloneSetup(postProviderController).build();
    }

    @Test
    void Provider를_등록한다() throws Exception {
        PostProviderCreateDto.Request request = new PostProviderCreateDto.Request("test_provider",
                                                                                  "https://test.com",
                                                                                  "test_description");

        Mockito.doReturn(mockPostProviderDto).when(postProviderService).create(any(PostProviderDto.class));

        mockMvc.perform(post("/collect/v1/providers")
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.providerId").value(mockPostProviderDto.id().toString()))
               .andExpect(jsonPath("$.createdAt").value(mockPostProviderDto.createdAt()));
    }

    @Test
    void Provider_목록을_조회한다() throws Exception {
        OffsetPageResult<PostProviderDto> pageResult = new OffsetPageResult<>(1L, 0, 10, List.of(mockPostProviderDto));

        Mockito.doReturn(pageResult).when(postProviderService).getPostProviders(0, 10);

        mockMvc.perform(get("/collect/v1/providers")
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.totalCount").value(1))
               .andExpect(jsonPath("$.items[0].providerId").value(mockPostProviderDto.id().toString()))
               .andExpect(jsonPath("$.items[0].name").value(mockPostProviderDto.name()))
               .andExpect(jsonPath("$.items[0].baseUrl").value(mockPostProviderDto.baseUrl()))
               .andExpect(jsonPath("$.items[0].description").value(mockPostProviderDto.description()));
    }

    @Test
    void Provider_한건을_조회한다() throws Exception {
        Mockito.doReturn(mockPostProviderDto).when(postProviderService).getPostProviderDto(any(UUID.class));

        mockMvc.perform(get("/collect/v1/providers/{id}", mockPostProviderDto.id())
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.providerId").value(mockPostProviderDto.id().toString()))
               .andExpect(jsonPath("$.name").value(mockPostProviderDto.name()))
               .andExpect(jsonPath("$.baseUrl").value(mockPostProviderDto.baseUrl()))
               .andExpect(jsonPath("$.description").value(mockPostProviderDto.description()))
               .andExpect(jsonPath("$.isUsed").value(mockPostProviderDto.isUsed()));
    }

    @Test
    void Provider를_수정한다() throws Exception {
        PostProviderUpdateDto.Request request = new PostProviderUpdateDto.Request("https://updated.com",
                                                                                  "updated_description",
                                                                                  true);

        Mockito.doNothing().when(postProviderService).update(any(PostProviderDto.class));
        Mockito.doReturn(mockPostProviderDto).when(postProviderService).getPostProviderDto(any(UUID.class));

        mockMvc.perform(patch("/collect/v1/providers/{id}", mockPostProviderDto.id())
                                .content(objectMapper.writeValueAsString(request))
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.providerId").value(mockPostProviderDto.id().toString()))
               .andExpect(jsonPath("$.updatedAt").value(mockPostProviderDto.updatedAt()));
    }

    @Test
    void Provider를_삭제한다() throws Exception {
        Mockito.doNothing().when(postProviderService).delete(any(UUID.class));

        mockMvc.perform(delete("/collect/v1/providers/{id}", mockPostProviderDto.id())
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isAccepted());
    }
}
