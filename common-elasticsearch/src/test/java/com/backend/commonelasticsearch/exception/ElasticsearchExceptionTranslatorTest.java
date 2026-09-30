package com.backend.commonelasticsearch.exception;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorResponse;
import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.BusinessException;
import com.backend.commondataaccess.exception.ErrorCode;
import com.backend.commondataaccess.exception.InfraException;
import com.backend.commonelasticsearch.operation.ElasticsearchOperation;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

@DisplayName("ElasticsearchExceptionTranslator 테스트")
class ElasticsearchExceptionTranslatorTest {

    private static ElasticsearchException elasticsearchException(int status, String type) {
        return new ElasticsearchException("test_endpoint",
                                          ErrorResponse.of(r -> r.status(status)
                                                                 .error(e -> e.type(type).reason("test_reason"))));
    }

    @DisplayName("일반 예외 변환")
    @Nested
    class GeneralExceptionTest {

        @Test
        void BusinessException은_그대로_반환한다() {
            // given
            BusinessException exception = new BadRequestException("test_message");

            // when
            BusinessException translated = ElasticsearchExceptionTranslator.translate(ElasticsearchOperation.GET,
                                                                                      exception);

            // then
            Assertions.assertThat(translated).isSameAs(exception);
        }

        @Test
        void IOException은_InfraException으로_변환한다() {
            // given
            IOException exception = new IOException("connection refused");

            // when
            BusinessException translated = ElasticsearchExceptionTranslator.translate(ElasticsearchOperation.SEARCH,
                                                                                      exception);

            // then
            Assertions.assertThat(translated).isInstanceOf(InfraException.class)
                      .hasCause(exception);
            Assertions.assertThat(translated.getErrorCode()).isEqualTo(ErrorCode.IE_ELASTICSEARCH_ERROR);
        }

        @Test
        void 예상하지_못한_예외는_InfraException으로_변환한다() {
            // given
            IllegalStateException exception = new IllegalStateException("unexpected");

            // when
            BusinessException translated = ElasticsearchExceptionTranslator.translate(ElasticsearchOperation.BULK,
                                                                                      exception);

            // then
            Assertions.assertThat(translated).isInstanceOf(InfraException.class)
                      .hasCause(exception);
            Assertions.assertThat(translated.getErrorCode()).isEqualTo(ErrorCode.IE_ELASTICSEARCH_ERROR);
        }
    }

    @DisplayName("ElasticsearchException 변환")
    @Nested
    class ElasticsearchExceptionTest {

        @ParameterizedTest
        @CsvSource({
                "500, unknown_exception",
                "503, mapper_parsing_exception",
                "404, index_not_found_exception",
                "429, circuit_breaking_exception",
                "300, unknown_exception"
        })
        void 클러스터_장애_성격이면_InfraException으로_변환한다(int status, String type) {
            // given
            ElasticsearchException exception = elasticsearchException(status, type);

            // when
            BusinessException translated = ElasticsearchExceptionTranslator.translate(ElasticsearchOperation.SEARCH,
                                                                                      exception);

            // then
            Assertions.assertThat(translated).isInstanceOf(InfraException.class)
                      .hasCause(exception);
            Assertions.assertThat(translated.getErrorCode()).isEqualTo(ErrorCode.IE_ELASTICSEARCH_ERROR);
        }

        @ParameterizedTest
        @CsvSource({
                "400, mapper_parsing_exception",
                "400, search_phase_execution_exception",
                "409, unknown_exception",
                "200, document_already_exists_exception"
        })
        void 요청_데이터_문제이면_BadRequestException으로_변환한다(int status, String type) {
            // given
            ElasticsearchException exception = elasticsearchException(status, type);

            // when
            BusinessException translated = ElasticsearchExceptionTranslator.translate(ElasticsearchOperation.GET,
                                                                                      exception);

            // then
            Assertions.assertThat(translated).isInstanceOf(BadRequestException.class);
            Assertions.assertThat(translated.getErrorCode()).isEqualTo(ErrorCode.BE_INVALID_INPUT_VALUE);
        }

        @Test
        void 응답과_에러_정보가_없으면_500으로_보고_InfraException으로_변환한다() {
            // given
            ElasticsearchException exception = Mockito.mock(ElasticsearchException.class);

            Mockito.doReturn(null).when(exception).response();
            Mockito.doReturn(null).when(exception).error();

            // when
            BusinessException translated = ElasticsearchExceptionTranslator.translate(ElasticsearchOperation.REINDEX,
                                                                                      exception);

            // then
            Assertions.assertThat(translated).isInstanceOf(InfraException.class)
                      .hasCause(exception);
        }
    }
}
