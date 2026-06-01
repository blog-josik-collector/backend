package com.backend.commonweb.error.security;

import com.backend.commondataaccess.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * 인증은 되었지만 권한이 없는 사용자가 API에 접근할 때 403 응답을 반환한다.
 */
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        errorResponseWriter.write(response,
                                  ErrorCode.BE_FORBIDDEN,
                                  ErrorCode.BE_FORBIDDEN.getDefaultMessage());
    }
}
