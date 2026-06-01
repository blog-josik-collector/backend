package com.backend.commondataaccess.exception;

public class InfraException extends BusinessException {

    public InfraException(ErrorCode errorCode) {
        super(errorCode);
    }

    public InfraException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }

    public InfraException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public InfraException(ErrorCode errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }
}
