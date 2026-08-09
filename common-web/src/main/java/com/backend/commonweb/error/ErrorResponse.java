package com.backend.commonweb.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "공통 에러 응답")
public record ErrorResponse(
        @Schema(description = "에러 코드", example = "BE40101")
        String code,

        @Schema(description = "에러 메시지", example = "인증되지 않은 사용자입니다.")
        String message,

        @Schema(description = "HTTP 상태 코드", example = "401")
        int status,

        @Schema(description = "에러 발생 시각")
        OffsetDateTime timestamp) {

    public static ErrorResponse of(String code, String message, int status) {
        return new ErrorResponse(code, message, status, OffsetDateTime.now());
    }
}
