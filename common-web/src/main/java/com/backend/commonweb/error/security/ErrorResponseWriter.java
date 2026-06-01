package com.backend.commonweb.error.security;

import com.backend.commondataaccess.exception.ErrorCode;
import com.backend.commonweb.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * ErrorResponse 생성과 HTTP 응답 직렬화를 한곳에서 처리한다. <br>
 * GlobalExceptionHandler 와 Security EntryPoint/Handler 가 동일한 JSON 형식을 사용하도록 한다.
 */
@Component
@RequiredArgsConstructor
public class ErrorResponseWriter {

    private final ObjectMapper objectMapper;

    public ErrorResponse create(ErrorCode errorCode, String message) {
        return ErrorResponse.of(errorCode.getCode(), message, errorCode.getStatus());
    }

    public ResponseEntity<ErrorResponse> toResponseEntity(ErrorCode errorCode, String message) {
        return ResponseEntity.status(errorCode.getStatus()).body(create(errorCode, message));
    }

    public void write(HttpServletResponse response, ErrorCode errorCode, String message) throws IOException {
        response.setStatus(errorCode.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getWriter(), create(errorCode, message));
    }
}
