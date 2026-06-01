package com.backend.commondataaccess.exception;

public class CrawlingException extends BusinessException {

    public CrawlingException() {
        super(ErrorCode.BE_CRAWLER_CONFLICT);
    }

    public CrawlingException(String message) {
        super(ErrorCode.BE_CRAWLER_CONFLICT, message);
    }
}
