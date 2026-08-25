package com.backend.userservice.client;

import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * interaction-service 내부 API 호출 클라이언트.
 * <p>
 * gateway({@code /interaction/v1/**})를 거치지 않고 서비스 간 직접 호출한다.
 */
@Slf4j
@Component
public class InteractionServiceClient {

    private final RestClient restClient;

    public InteractionServiceClient(RestClient.Builder restClientBuilder,
                                    @Value("${interaction-service.base-url:http://localhost:8083}") String baseUrl) {

        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
    }

    /**
     * 탈퇴 사용자의 댓글/대댓글, 좋아요, 즐겨찾기를 soft-delete 한다.
     */
    public void softDeleteUserInteractions(UUID userId) {
        restClient.delete()
                  .uri("/interaction/internal/v1/users/{userId}/interactions", userId)
                  .retrieve()
                  .toBodilessEntity();
    }
}
