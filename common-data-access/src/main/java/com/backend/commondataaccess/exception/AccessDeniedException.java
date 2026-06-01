package com.backend.commondataaccess.exception;

public class AccessDeniedException extends BusinessException {

    public AccessDeniedException() {
        super(ErrorCode.BE_FORBIDDEN);
    }

    public AccessDeniedException(String message) {
        super(ErrorCode.BE_FORBIDDEN, message);
    }
}
