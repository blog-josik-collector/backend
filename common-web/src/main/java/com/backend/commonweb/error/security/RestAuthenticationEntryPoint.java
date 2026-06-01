package com.backend.commonweb.error.security;

import com.backend.commondataaccess.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 인증되지 않은 사용자가 보호된 API에 접근할 때 401 응답을 반환한다.
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ErrorResponseWriter errorResponseWriter;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        errorResponseWriter.write(response,
                                  ErrorCode.BE_UNAUTHORIZED,
                                  ErrorCode.BE_UNAUTHORIZED.getDefaultMessage());
    }
}
