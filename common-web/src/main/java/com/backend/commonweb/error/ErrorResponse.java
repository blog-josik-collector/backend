package com.backend.commonweb.error;

import java.time.OffsetDateTime;

public record ErrorResponse(String code,
                            String message,
                            int status,
                            OffsetDateTime timestamp) {

    public static ErrorResponse of(String code, String message, int status) {
        return new ErrorResponse(code, message, status, OffsetDateTime.now());
    }
}
