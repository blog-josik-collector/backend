package com.backend.commondataaccess.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * BE: Backend-Exception <br> FE: Framework-Exception <br> IE: Infra-Exception
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // Backend-Exception
    BE_INVALID_INPUT_VALUE(400, "BE40001", "입력 데이터에 문제가 있습니다."),
    BE_UNAUTHORIZED(401, "BE40101", "인증되지 않은 사용자입니다."),
    BE_FORBIDDEN(403, "BE40301", "권한이 없는 사용자입니다."),
    BE_NOT_FOUND(404, "BE40401", "리소스를 찾을 수 없습니다."),
    BE_CONFLICT(409, "BE40901", "데이터 상태가 유효하지 않습니다."),
    BE_CRAWLER_CONFLICT(409, "BE40902", "크롤링 대상 사이트의 데이터가 유효하지 않습니다."),
    BE_INTERNAL_ERROR(500, "BE50001", "서버 처리 오류(관리자에게 문의하세요)"),

    // Framework-Exception
    FE_INVALID_INPUT_VALUE(400, "FE40001", "입력 데이터에 문제가 있습니다."),
    FE_METHOD_NOT_ALLOWED(405, "FE40501", "지원하지 않는 HTTP 메서드입니다."),
    FE_CONFLICT(409, "FE40901", "데이터 상태가 유효하지 않습니다."),
    FE_UNSUPPORTED_MEDIA_TYPE(415, "FE41501", "지원하지 않는 Content-Type 입니다."),
    FE_UNHANDLED_ERROR(500, "FE50001", "서버 내부 오류가 발생했습니다."),

    // Infra-Exception
    IE_ELASTICSEARCH_ERROR(500, "IE50001", "서버 처리 오류(관리자에게 문의하세요)"),
    IE_REDIS_ERROR(500, "IE50002", "서버 처리 오류(관리자에게 문의하세요)"),
    IE_POSTGRESQL_ERROR(500, "IE50003", "서버 처리 오류(관리자에게 문의하세요)"),
    IE_GOOGLE_AUTH_ERROR(500, "IE50004", "서버 처리 오류(관리자에게 문의하세요)");

    private final int status;
    private final String code;
    private final String defaultMessage;
}
