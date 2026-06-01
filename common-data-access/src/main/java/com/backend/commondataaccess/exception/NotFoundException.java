package com.backend.commondataaccess.exception;

public class NotFoundException extends BusinessException {

    public NotFoundException() {
        super(ErrorCode.BE_NOT_FOUND);
    }

    public NotFoundException(String message) {
        super(ErrorCode.BE_NOT_FOUND, message);
    }
}
