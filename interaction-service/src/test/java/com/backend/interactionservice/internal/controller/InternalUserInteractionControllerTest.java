package com.backend.interactionservice.internal.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.backend.interactionservice.internal.service.InternalUserInteractionService;
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

@DisplayName("InternalUserInteractionController 테스트")
@ExtendWith(MockitoExtension.class)
class InternalUserInteractionControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private InternalUserInteractionController internalUserInteractionController;

    @Mock
    private InternalUserInteractionService internalUserInteractionService;

    @BeforeEach
    void init() {
        mockMvc = MockMvcBuilders.standaloneSetup(internalUserInteractionController).build();
    }

    @Test
    void 회원의_interaction_리소스를_soft_delete한다() throws Exception {
        UUID userId = UUID.randomUUID();

        Mockito.doNothing().when(internalUserInteractionService).softDeleteByUserId(userId);

        mockMvc.perform(delete("/interaction/internal/v1/users/{userId}/interactions", userId)
                                .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isNoContent());

        Mockito.verify(internalUserInteractionService).softDeleteByUserId(userId);
    }
}
