package com.backend.commonelasticsearch.exception;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import com.backend.commondataaccess.exception.BadRequestException;
import com.backend.commondataaccess.exception.BusinessException;
import com.backend.commondataaccess.exception.ErrorCode;
import com.backend.commondataaccess.exception.InfraException;
import com.backend.commonelasticsearch.operation.ElasticsearchOperation;
import java.io.IOException;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Elasticsearch client 예외를 애플리케이션 {@link BusinessException} 으로 변환한다. <br>
 * - 요청 데이터/쿼리 문제(4xx 성격) → {@link BadRequestException} (400) <br>
 * - 통신 장애/클러스터 장애(5xx 성격) → {@link InfraException} (500)
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ElasticsearchExceptionTranslator {

    private static final Set<String> INFRA_ERROR_TYPES = Set.of(
            "index_not_found_exception",
            "cluster_block_exception",
            "no_shard_available_exception",
            "unavailable_exception",
            "master_not_discovered_exception",
            "node_closed_exception",
            "timeout_exception",
            "circuit_breaking_exception"
    );

    private static final Set<String> CLIENT_ERROR_TYPES = Set.of(
            "mapper_parsing_exception",
            "illegal_argument_exception",
            "query_shard_exception",
            "x_content_parse_exception",
            "parsing_exception",
            "strict_dynamic_mapping_exception",
            "document_parsing_exception",
            "validation_exception",
            "search_phase_execution_exception",
            "document_already_exists_exception"
    );

    public static BusinessException translate(ElasticsearchOperation operation, Exception exception) {
        if (exception instanceof BusinessException businessException) {
            return businessException;
        }
        if (exception instanceof IOException ioException) {
            log.error("[ES][IE50001] {} communication failure", operation.name(), ioException);
            return new InfraException(ErrorCode.IE_ELASTICSEARCH_ERROR, ioException);
        }
        if (exception instanceof ElasticsearchException elasticsearchException) {
            return translateElasticsearchException(operation, elasticsearchException);
        }

        log.error("[ES][IE50001] {} unexpected failure", operation.name(), exception);
        return new InfraException(ErrorCode.IE_ELASTICSEARCH_ERROR, exception);
    }

    private static BusinessException translateElasticsearchException(ElasticsearchOperation operation,
                                                                     ElasticsearchException exception) {

        int status = exception.response() != null ? exception.response().status() : 500;
        String type = errorType(exception);
        String reason = errorReason(exception);

        log.error("[ES][IE50001] {} failed status={} type={} reason={}",
                  operation.name(), status, type, reason, exception);

        if (isInfraError(status, type)) {
            return new InfraException(ErrorCode.IE_ELASTICSEARCH_ERROR, exception);
        }
        if (isClientDataError(status, type)) {
            return new BadRequestException(ErrorCode.BE_INVALID_INPUT_VALUE.getDefaultMessage());
        }
        if (status >= 500) {
            return new InfraException(ErrorCode.IE_ELASTICSEARCH_ERROR, exception);
        }
        if (status >= 400) {
            return new BadRequestException(ErrorCode.BE_INVALID_INPUT_VALUE.getDefaultMessage());
        }

        return new InfraException(ErrorCode.IE_ELASTICSEARCH_ERROR, exception);
    }

    private static boolean isInfraError(int status, String type) {
        if (status >= 500) {
            return true;
        }
        return type != null && INFRA_ERROR_TYPES.contains(type);
    }

    private static boolean isClientDataError(int status, String type) {
        if (type != null && CLIENT_ERROR_TYPES.contains(type)) {
            return true;
        }
        return status >= 400 && status < 500;
    }

    private static String errorType(ElasticsearchException exception) {
        ErrorCause error = exception.error();
        return error != null ? error.type() : null;
    }

    private static String errorReason(ElasticsearchException exception) {
        ErrorCause error = exception.error();
        return error != null ? error.reason() : exception.getMessage();
    }
}
