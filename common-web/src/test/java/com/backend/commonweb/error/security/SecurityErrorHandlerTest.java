package com.backend.commonweb.error.security;

import com.backend.commondataaccess.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

@DisplayName("Security 에러 핸들러 테스트")
@ExtendWith(MockitoExtension.class)
class SecurityErrorHandlerTest {

    @Mock
    private ErrorResponseWriter errorResponseWriter;

    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void 미인증_요청이면_401_응답을_쓴다() throws Exception {
        RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(errorResponseWriter);

        entryPoint.commence(request, response, new InsufficientAuthenticationException("no auth"));

        Mockito.verify(errorResponseWriter)
               .write(response, ErrorCode.BE_UNAUTHORIZED, ErrorCode.BE_UNAUTHORIZED.getDefaultMessage());
    }

    @Test
    void 권한이_없는_요청이면_403_응답을_쓴다() throws Exception {
        RestAccessDeniedHandler accessDeniedHandler = new RestAccessDeniedHandler(errorResponseWriter);

        accessDeniedHandler.handle(request, response, new AccessDeniedException("denied"));

        Mockito.verify(errorResponseWriter)
               .write(response, ErrorCode.BE_FORBIDDEN, ErrorCode.BE_FORBIDDEN.getDefaultMessage());
    }
}
