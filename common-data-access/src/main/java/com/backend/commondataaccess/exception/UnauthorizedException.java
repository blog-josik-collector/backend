package com.backend.commondataaccess.exception;

public class UnauthorizedException extends BusinessException {

    public UnauthorizedException() {
        super(ErrorCode.BE_UNAUTHORIZED);
    }

    public UnauthorizedException(String message) {
        super(ErrorCode.BE_UNAUTHORIZED, message);
    }
}
