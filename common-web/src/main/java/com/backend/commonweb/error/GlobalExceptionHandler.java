package com.backend.commonweb.error;

import static com.backend.commondataaccess.exception.ErrorCode.FE_CONFLICT;
import static com.backend.commondataaccess.exception.ErrorCode.FE_INVALID_INPUT_VALUE;
import static com.backend.commondataaccess.exception.ErrorCode.FE_METHOD_NOT_ALLOWED;
import static com.backend.commondataaccess.exception.ErrorCode.FE_UNHANDLED_ERROR;
import static com.backend.commondataaccess.exception.ErrorCode.FE_UNSUPPORTED_MEDIA_TYPE;

import com.backend.commondataaccess.exception.BusinessException;
import com.backend.commonweb.error.security.ErrorResponseWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ErrorResponseWriter errorResponseWriter;

    @ExceptionHandler({
            IllegalArgumentException.class, TypeMismatchException.class,
            HttpMessageNotReadableException.class, MissingServletRequestParameterException.class,
            MultipartException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestException(Exception e) {
        log.debug("Bad request exception occurred: {}", e.getMessage(), e);
        return errorResponseWriter.toResponseEntity(FE_INVALID_INPUT_VALUE, e.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowedException(Exception e) {
        return errorResponseWriter.toResponseEntity(FE_METHOD_NOT_ALLOWED, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(Exception e) {
        return errorResponseWriter.toResponseEntity(FE_CONFLICT, e.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeException(Exception e) {
        return errorResponseWriter.toResponseEntity(FE_UNSUPPORTED_MEDIA_TYPE, e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        return errorResponseWriter.toResponseEntity(e.getErrorCode(), e.getErrorMessage());
    }

    @ExceptionHandler({Exception.class, RuntimeException.class})
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unexpected exception occurred: {}", e.getMessage(), e);
        return errorResponseWriter.toResponseEntity(FE_UNHANDLED_ERROR, FE_UNHANDLED_ERROR.getDefaultMessage());
    }
}
