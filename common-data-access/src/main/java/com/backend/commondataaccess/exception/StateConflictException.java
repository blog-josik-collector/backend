package com.backend.commondataaccess.exception;

public class StateConflictException extends BusinessException {

    public StateConflictException() {
        super(ErrorCode.BE_CONFLICT);
    }

    public StateConflictException(String message) {
        super(ErrorCode.BE_CONFLICT, message);
    }

    public StateConflictException(Throwable cause, String message) {
        super(ErrorCode.BE_CONFLICT, message, cause);
    }
}
