package com.backend.commondataaccess.exception;

public class BadRequestException extends BusinessException {

    public BadRequestException() {
        super(ErrorCode.BE_INVALID_INPUT_VALUE);
    }

    public BadRequestException(String message) {
        super(ErrorCode.BE_INVALID_INPUT_VALUE, message);
    }
}
