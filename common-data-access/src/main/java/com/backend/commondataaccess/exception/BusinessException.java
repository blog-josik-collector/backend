package com.backend.commondataaccess.exception;

import lombok.Getter;

@Getter
public abstract class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String errorMessage;

    // 1. 기본 생성자 (메시지가 없는 경우)
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDefaultMessage();
    }

    // 2. 에러 메시지만 전달하는 생성자 (가장 많이 사용)
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.errorMessage = message;
    }

    // 3. 에러 메시지와 예외 원인(다른 Exception)을 함께 전달하는 생성자
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.errorMessage = message;
    }

    // 4. 예외 원인만 전달하는 생성자
    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
        this.errorMessage = errorCode.getDefaultMessage();
    }
}
