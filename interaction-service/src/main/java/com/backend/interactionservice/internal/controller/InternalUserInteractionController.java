package com.backend.interactionservice.internal.controller;

import com.backend.interactionservice.internal.service.InternalUserInteractionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서비스 간 호출 전용. gateway({@code /interaction/v1/**}) 경로 밖에 두어 외부 노출을 피한다.
 */
@RequestMapping("/interaction/internal/v1")
@RestController
@RequiredArgsConstructor
public class InternalUserInteractionController {

    private final InternalUserInteractionService internalUserInteractionService;

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/users/{userId}/interactions")
    public ResponseEntity<Void> softDeleteUserInteractions(@PathVariable UUID userId) {
        internalUserInteractionService.softDeleteByUserId(userId);
        return ResponseEntity.noContent().build();
    }
}
