package com.backend.userservice.client;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;

import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

@DisplayName("InteractionServiceClient 테스트")
class InteractionServiceClientTest {

    private static final String BASE_URL = "http://interaction-service:8083";

    private MockRestServiceServer mockServer;

    private InteractionServiceClient interactionServiceClient;

    @BeforeEach
    void init() {
        RestClient.Builder restClientBuilder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(restClientBuilder).build();
        interactionServiceClient = new InteractionServiceClient(restClientBuilder, BASE_URL);
    }

    @Test
    void 탈퇴_사용자의_interaction_삭제_API를_호출한다() {
        // given
        UUID userId = UUID.randomUUID();

        mockServer.expect(requestTo(BASE_URL + "/interaction/internal/v1/users/" + userId + "/interactions"))
                  .andExpect(method(HttpMethod.DELETE))
                  .andRespond(withNoContent());

        // when
        interactionServiceClient.softDeleteUserInteractions(userId);

        // then
        mockServer.verify();
    }

    @Test
    void interaction_service가_실패하면_예외가_전파된다() {
        // given
        UUID userId = UUID.randomUUID();

        mockServer.expect(requestTo(BASE_URL + "/interaction/internal/v1/users/" + userId + "/interactions"))
                  .andExpect(method(HttpMethod.DELETE))
                  .andRespond(withServerError());

        // when & then
        Assertions.assertThatThrownBy(() -> interactionServiceClient.softDeleteUserInteractions(userId))
                  .isInstanceOf(HttpServerErrorException.class);
        mockServer.verify();
    }
}
