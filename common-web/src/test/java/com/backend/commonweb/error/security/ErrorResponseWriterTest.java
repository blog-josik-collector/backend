package com.backend.commonweb.error.security;

import com.backend.commondataaccess.exception.ErrorCode;
import com.backend.commonweb.error.ErrorResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("ErrorResponseWriter 테스트")
class ErrorResponseWriterTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final ErrorResponseWriter writer = new ErrorResponseWriter(objectMapper);

    @Test
    void ErrorCode와_메시지로_ErrorResponse를_만든다() {
        ErrorResponse errorResponse = writer.create(ErrorCode.BE_NOT_FOUND, "custom");

        Assertions.assertThat(errorResponse.code()).isEqualTo("BE40401");
        Assertions.assertThat(errorResponse.message()).isEqualTo("custom");
        Assertions.assertThat(errorResponse.status()).isEqualTo(404);
        Assertions.assertThat(errorResponse.timestamp()).isNotNull();
    }

    @Test
    void ErrorCode의_상태코드로_ResponseEntity를_만든다() {
        ResponseEntity<ErrorResponse> responseEntity = writer.toResponseEntity(ErrorCode.BE_CONFLICT, "custom");

        Assertions.assertThat(responseEntity.getStatusCode().value()).isEqualTo(409);
        Assertions.assertThat(responseEntity.getBody()).isNotNull();
        Assertions.assertThat(responseEntity.getBody().code()).isEqualTo("BE40901");
        Assertions.assertThat(responseEntity.getBody().message()).isEqualTo("custom");
    }

    @Test
    void HttpServletResponse에_UTF8_JSON으로_ErrorResponse를_쓴다() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        writer.write(response, ErrorCode.BE_UNAUTHORIZED, ErrorCode.BE_UNAUTHORIZED.getDefaultMessage());

        Assertions.assertThat(response.getStatus()).isEqualTo(401);
        Assertions.assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        Assertions.assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");

        JsonNode body = objectMapper.readTree(response.getContentAsString());
        Assertions.assertThat(body.get("code").asText()).isEqualTo("BE40101");
        Assertions.assertThat(body.get("message").asText()).isEqualTo(ErrorCode.BE_UNAUTHORIZED.getDefaultMessage());
        Assertions.assertThat(body.get("status").asInt()).isEqualTo(401);
        Assertions.assertThat(body.hasNonNull("timestamp")).isTrue();
    }
}
