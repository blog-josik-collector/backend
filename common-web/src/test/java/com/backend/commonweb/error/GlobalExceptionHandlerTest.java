package com.backend.commonweb.error;

import com.backend.commondataaccess.exception.ErrorCode;
import com.backend.commondataaccess.exception.NotFoundException;
import com.backend.commonweb.error.security.ErrorResponseWriter;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;

@DisplayName("GlobalExceptionHandler 테스트")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler(new ErrorResponseWriter(Jackson2ObjectMapperBuilder.json().build()));

    @Test
    void 잘못된_입력이면_FE40001로_응답한다() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequestException(new IllegalArgumentException("bad"));

        assertResponse(response, ErrorCode.FE_INVALID_INPUT_VALUE, "bad");
    }

    @Test
    void 지원하지_않는_HTTP_메서드면_FE40501로_응답한다() {
        HttpRequestMethodNotSupportedException e = new HttpRequestMethodNotSupportedException("PATCH");

        ResponseEntity<ErrorResponse> response = handler.handleMethodNotAllowedException(e);

        assertResponse(response, ErrorCode.FE_METHOD_NOT_ALLOWED, e.getMessage());
    }

    @Test
    void IllegalStateException이면_FE40901로_응답한다() {
        ResponseEntity<ErrorResponse> response = handler.handleIllegalStateException(new IllegalStateException("state"));

        assertResponse(response, ErrorCode.FE_CONFLICT, "state");
    }

    @Test
    void 지원하지_않는_ContentType이면_FE41501로_응답한다() {
        HttpMediaTypeNotSupportedException e =
                new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON));

        ResponseEntity<ErrorResponse> response = handler.handleHttpMediaTypeException(e);

        assertResponse(response, ErrorCode.FE_UNSUPPORTED_MEDIA_TYPE, e.getMessage());
    }

    @Test
    void BusinessException이면_예외의_ErrorCode와_메시지로_응답한다() {
        ResponseEntity<ErrorResponse> response = handler.handleBusinessException(new NotFoundException("없음"));

        assertResponse(response, ErrorCode.BE_NOT_FOUND, "없음");
    }

    @Test
    void 처리되지_않은_예외는_내부_메시지를_숨기고_FE50001로_응답한다() {
        ResponseEntity<ErrorResponse> response = handler.handleException(new RuntimeException("internal detail"));

        assertResponse(response, ErrorCode.FE_UNHANDLED_ERROR, ErrorCode.FE_UNHANDLED_ERROR.getDefaultMessage());
    }

    private static void assertResponse(ResponseEntity<ErrorResponse> response, ErrorCode errorCode, String message) {
        Assertions.assertThat(response.getStatusCode().value()).isEqualTo(errorCode.getStatus());
        Assertions.assertThat(response.getBody()).isNotNull();
        Assertions.assertThat(response.getBody().code()).isEqualTo(errorCode.getCode());
        Assertions.assertThat(response.getBody().message()).isEqualTo(message);
        Assertions.assertThat(response.getBody().status()).isEqualTo(errorCode.getStatus());
    }
}
