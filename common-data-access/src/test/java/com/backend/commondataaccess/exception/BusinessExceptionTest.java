package com.backend.commondataaccess.exception;

import java.util.function.Supplier;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("BusinessException 테스트")
class BusinessExceptionTest {

    private static final Throwable CAUSE = new RuntimeException("cause");

    @DisplayName("기본 생성자")
    @Nested
    class DefaultConstructorTest {

        @ParameterizedTest
        @MethodSource("com.backend.commondataaccess.exception.BusinessExceptionTest#defaultExceptions")
        void ErrorCode의_기본_메시지를_사용한다(Supplier<BusinessException> supplier, ErrorCode errorCode) {
            BusinessException exception = supplier.get();

            Assertions.assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            Assertions.assertThat(exception.getErrorMessage()).isEqualTo(errorCode.getDefaultMessage());
            Assertions.assertThat(exception.getMessage()).isEqualTo(errorCode.getDefaultMessage());
        }
    }

    @DisplayName("메시지 생성자")
    @Nested
    class MessageConstructorTest {

        @ParameterizedTest
        @MethodSource("com.backend.commondataaccess.exception.BusinessExceptionTest#messageExceptions")
        void 전달한_메시지를_사용한다(Supplier<BusinessException> supplier, ErrorCode errorCode) {
            BusinessException exception = supplier.get();

            Assertions.assertThat(exception.getErrorCode()).isEqualTo(errorCode);
            Assertions.assertThat(exception.getErrorMessage()).isEqualTo("custom");
            Assertions.assertThat(exception.getMessage()).isEqualTo("custom");
        }
    }

    @DisplayName("원인 예외 생성자")
    @Nested
    class CauseConstructorTest {

        @Test
        void 메시지와_원인을_함께_전달하면_둘_다_보관한다() {
            BusinessException crawling = new CrawlingException("custom", CAUSE);
            BusinessException conflict = new StateConflictException(CAUSE, "custom");
            BusinessException infra = new InfraException(ErrorCode.IE_REDIS_ERROR, "custom", CAUSE);

            Assertions.assertThat(crawling.getErrorCode()).isEqualTo(ErrorCode.BE_CRAWLER_CONFLICT);
            Assertions.assertThat(conflict.getErrorCode()).isEqualTo(ErrorCode.BE_CONFLICT);
            Assertions.assertThat(infra.getErrorCode()).isEqualTo(ErrorCode.IE_REDIS_ERROR);
            Assertions.assertThat(Stream.of(crawling, conflict, infra))
                      .allSatisfy(exception -> {
                          Assertions.assertThat(exception.getErrorMessage()).isEqualTo("custom");
                          Assertions.assertThat(exception.getCause()).isSameAs(CAUSE);
                      });
        }

        @Test
        void 원인만_전달하면_ErrorCode의_기본_메시지를_사용한다() {
            BusinessException exception = new InfraException(ErrorCode.IE_POSTGRESQL_ERROR, CAUSE);

            Assertions.assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.IE_POSTGRESQL_ERROR);
            Assertions.assertThat(exception.getErrorMessage()).isEqualTo(ErrorCode.IE_POSTGRESQL_ERROR.getDefaultMessage());
            Assertions.assertThat(exception.getCause()).isSameAs(CAUSE);
        }
    }

    static Stream<Arguments> defaultExceptions() {
        return Stream.of(
                Arguments.of((Supplier<BusinessException>) BadRequestException::new, ErrorCode.BE_INVALID_INPUT_VALUE),
                Arguments.of((Supplier<BusinessException>) UnauthorizedException::new, ErrorCode.BE_UNAUTHORIZED),
                Arguments.of((Supplier<BusinessException>) AccessDeniedException::new, ErrorCode.BE_FORBIDDEN),
                Arguments.of((Supplier<BusinessException>) NotFoundException::new, ErrorCode.BE_NOT_FOUND),
                Arguments.of((Supplier<BusinessException>) StateConflictException::new, ErrorCode.BE_CONFLICT),
                Arguments.of((Supplier<BusinessException>) CrawlingException::new, ErrorCode.BE_CRAWLER_CONFLICT),
                Arguments.of((Supplier<BusinessException>) () -> new InfraException(ErrorCode.IE_ELASTICSEARCH_ERROR),
                             ErrorCode.IE_ELASTICSEARCH_ERROR)
        );
    }

    static Stream<Arguments> messageExceptions() {
        return Stream.of(
                Arguments.of((Supplier<BusinessException>) () -> new BadRequestException("custom"), ErrorCode.BE_INVALID_INPUT_VALUE),
                Arguments.of((Supplier<BusinessException>) () -> new UnauthorizedException("custom"), ErrorCode.BE_UNAUTHORIZED),
                Arguments.of((Supplier<BusinessException>) () -> new AccessDeniedException("custom"), ErrorCode.BE_FORBIDDEN),
                Arguments.of((Supplier<BusinessException>) () -> new NotFoundException("custom"), ErrorCode.BE_NOT_FOUND),
                Arguments.of((Supplier<BusinessException>) () -> new StateConflictException("custom"), ErrorCode.BE_CONFLICT),
                Arguments.of((Supplier<BusinessException>) () -> new CrawlingException("custom"), ErrorCode.BE_CRAWLER_CONFLICT),
                Arguments.of((Supplier<BusinessException>) () -> new InfraException(ErrorCode.IE_GOOGLE_AUTH_ERROR, "custom"),
                             ErrorCode.IE_GOOGLE_AUTH_ERROR)
        );
    }
}
